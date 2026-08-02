package io.github.fushuwei.scaskeleton.datasource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverPageRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverUpdateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverOptionResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverUploadResponse;
import io.github.fushuwei.scaskeleton.datasource.converter.DriverConverter;
import io.github.fushuwei.scaskeleton.datasource.engine.driver.DriverClassDetector;
import io.github.fushuwei.scaskeleton.datasource.engine.storage.DriverStore;
import io.github.fushuwei.scaskeleton.datasource.engine.storage.DriverStoreProperties;
import io.github.fushuwei.scaskeleton.datasource.entity.Driver;
import io.github.fushuwei.scaskeleton.datasource.mapper.DriverMapper;
import io.github.fushuwei.scaskeleton.datasource.service.DriverService;
import io.github.fushuwei.scaskeleton.mybatis.reference.ReferenceChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 驱动管理 Service 实现。
 * <p>
 * 采用 Chat2DB 风格的目录式管理：每个驱动记录对应一个目录 drivers/{driverName}/，
 * 目录下存放驱动主 JAR + 所有依赖 JAR，加载时用一个 URLClassLoader 全部加载。
 * <p>
 * 上传流程：支持多 JAR 上传 → 临时存到 drivers/_temp/{uploadId}/ → 探测驱动类 → 返回 uploadId。
 * 创建流程：用 driverName 作为目录名 → 把临时目录重命名为 drivers/{driverName}/ → 落库。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private static final String TEMP_DIR_PREFIX = "drivers/_temp/";
    private static final String DRIVER_DIR_PREFIX = "drivers/";

    private final DriverMapper driverMapper;
    private final DriverConverter driverConverter;
    private final ReferenceChecker referenceChecker;
    private final DriverStore driverStore;
    private final DriverStoreProperties driverStoreProperties;

    @Override
    public IPage<DriverResponse> pageDrivers(DriverPageRequest request) {
        Page<Driver> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Driver> wrapper = new LambdaQueryWrapper<>();

        if (request.getDbType() != null) {
            wrapper.eq(Driver::getDbType, request.getDbType().name());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Driver::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(Driver::getDriverName, request.getKeyword())
                .or().like(Driver::getDriverClass, request.getKeyword()));
        }
        wrapper.orderByDesc(Driver::getCreateTime);

        IPage<Driver> driverPage = driverMapper.selectPage(page, wrapper);
        return driverPage.convert(driverConverter::toDriverResponse);
    }

    @Override
    public DriverResponse getDriverById(String id) {
        Driver driver = loadDriverEntity(id);
        return driverConverter.toDriverResponse(driver);
    }

    /**
     * 上传驱动 JAR 文件（支持多文件）。
     * <p>
     * 上传的 JAR 会临时存到 drivers/_temp/{uploadId}/ 目录，前端拿到 uploadId + 探测到的 driverClass 后回填到创建表单。
     * 创建驱动时用 driverName 作为正式目录名，把临时目录重命名为 drivers/{driverName}/。
     */
    @Override
    public DriverUploadResponse uploadDriver(DbType dbType, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "上传文件不能为空");
        }

        // 生成上传批次 ID 作为临时目录名
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        String tempDriverDir = TEMP_DIR_PREFIX + uploadId;

        List<String> jarFileNames = new ArrayList<>();
        long totalSize = 0;
        String firstJarSha256 = null;
        List<Path> localJarPaths = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.endsWith(".jar")) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "仅支持 .jar 文件: " + originalName);
            }

            // 计算 SHA256（首个 JAR 用于展示）
            String sha256;
            try (InputStream is = file.getInputStream()) {
                sha256 = computeSha256(is);
            } catch (IOException e) {
                throw new BusinessException(ResultCode.FAILURE, "计算 SHA256 失败: " + e.getMessage());
            }
            if (firstJarSha256 == null) {
                firstJarSha256 = sha256;
            }

            // 存储到临时目录
            try (InputStream is = file.getInputStream()) {
                driverStore.putJar(tempDriverDir, originalName, is, file.getSize());
            } catch (IOException e) {
                throw new BusinessException(ResultCode.FAILURE, "存储驱动 JAR 失败: " + e.getMessage());
            }

            jarFileNames.add(originalName);
            totalSize += file.getSize();
        }

        if (jarFileNames.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "没有有效的 JAR 文件");
        }

        // 获取本地路径用于探测驱动类
        localJarPaths.addAll(driverStore.listLocalJars(tempDriverDir));
        Path[] jarPathArray = localJarPaths.toArray(new Path[0]);

        List<String> detectedClasses = DriverClassDetector.detectDriverClasses(jarPathArray);
        String driverClass = detectedClasses.isEmpty() ? null : detectedClasses.get(0);

        DriverUploadResponse response = new DriverUploadResponse();
        response.setUploadId(uploadId);
        response.setJarSha256(firstJarSha256);
        response.setFileSize(totalSize);
        response.setJarFileNames(jarFileNames);
        response.setDetectedDriverClasses(detectedClasses);
        response.setDriverClass(driverClass);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDriver(DriverCreateRequest request) {
        // 校验驱动名称唯一（作为目录名必须唯一）
        long existCount = driverMapper.selectCount(new LambdaQueryWrapper<Driver>()
            .eq(Driver::getDriverName, request.getDriverName()));
        if (existCount > 0) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "驱动名称已存在: " + request.getDriverName());
        }

        // 临时目录 → 正式目录
        String tempDriverDir = TEMP_DIR_PREFIX + request.getUploadId();
        String driverDir = DRIVER_DIR_PREFIX + request.getDriverName();

        // 获取临时目录下 JAR 信息（用于落库 fileSize）
        List<Path> jarPaths = driverStore.listLocalJars(tempDriverDir);
        long totalSize = jarPaths.stream().mapToLong(p -> {
            try {
                return java.nio.file.Files.size(p);
            } catch (IOException e) {
                return 0;
            }
        }).sum();

        // 重命名临时目录为正式目录
        driverStore.renameDriverDir(tempDriverDir, driverDir);

        // 落库
        Driver driver = driverConverter.toDriver(request);
        driver.setDbType(request.getDbType().name());
        driver.setObjectKey(driverDir);
        driver.setFileSize(totalSize);
        driver.setStatus("enabled");
        driver.setIsBuiltin(0);
        driver.setStorageType(driverStoreProperties.getStorageType());
        driverMapper.insert(driver);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDriver(DriverUpdateRequest request) {
        Driver driver = loadDriverEntity(request.getId());

        if (StringUtils.hasText(request.getDriverName())) {
            driver.setDriverName(request.getDriverName());
        }
        if (StringUtils.hasText(request.getUrlTemplate())) {
            driver.setUrlTemplate(request.getUrlTemplate());
        }
        if (StringUtils.hasText(request.getAllowedParams())) {
            driver.setAllowedParams(request.getAllowedParams());
        }
        if (StringUtils.hasText(request.getRemark())) {
            driver.setRemark(request.getRemark());
        }
        driver.setVersion(request.getVersion());

        int affectedRows = driverMapper.updateById(driver);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDriver(String id) {
        Driver driver = loadDriverEntity(id);
        if (driver.getIsBuiltin() != null && driver.getIsBuiltin() == 1) {
            throw new BusinessException("内置驱动不允许删除");
        }

        // 引用校验：检查是否被数据源引用
        referenceChecker.check(Driver.class, id);

        driverMapper.deleteById(id);

        // 存储清理：删除整个驱动目录
        try {
            driverStore.deleteDriverDir(driver.getObjectKey());
        } catch (Exception e) {
            log.warn("删除驱动目录失败: objectKey={}, error={}", driver.getObjectKey(), e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDrivers(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Driver> drivers = driverMapper.selectBatchIds(ids);
        if (drivers.isEmpty()) {
            return;
        }

        // 内置驱动不允许删除
        List<Driver> builtinDrivers = drivers.stream()
            .filter(d -> d.getIsBuiltin() != null && d.getIsBuiltin() == 1)
            .toList();
        if (!builtinDrivers.isEmpty()) {
            throw new BusinessException("内置驱动不允许删除: " +
                builtinDrivers.stream().map(Driver::getDriverName).collect(Collectors.joining(", ")));
        }

        // 批量引用校验
        referenceChecker.checkBatch(Driver.class, ids);

        driverMapper.deleteBatchIds(ids);

        // 存储清理：删除每个驱动目录
        for (Driver driver : drivers) {
            try {
                driverStore.deleteDriverDir(driver.getObjectKey());
            } catch (Exception e) {
                log.warn("删除驱动目录失败: objectKey={}, error={}", driver.getObjectKey(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(String id, String status) {
        Driver driver = loadDriverEntity(id);
        driver.setStatus(status);
        int affectedRows = driverMapper.updateById(driver);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }
    }

    @Override
    public List<DriverOptionResponse> listDriverOptions(DbType dbType) {
        LambdaQueryWrapper<Driver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Driver::getStatus, "enabled");
        if (dbType != null) {
            wrapper.eq(Driver::getDbType, dbType.name());
        }
        wrapper.orderByDesc(Driver::getCreateTime);

        List<Driver> drivers = driverMapper.selectList(wrapper);
        return drivers.stream()
            .map(d -> new DriverOptionResponse(d.getId(), d.getDriverName(), d.getDriverVersion()))
            .toList();
    }

    private Driver loadDriverEntity(String id) {
        Driver driver = driverMapper.selectById(id);
        if (driver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "驱动不存在");
        }
        return driver;
    }

    private String computeSha256(InputStream is) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 算法不可用", e);
        }
    }
}
