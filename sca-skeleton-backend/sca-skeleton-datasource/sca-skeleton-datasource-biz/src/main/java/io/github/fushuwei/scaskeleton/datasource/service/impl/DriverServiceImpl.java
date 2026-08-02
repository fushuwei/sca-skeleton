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
import java.util.HexFormat;
import java.util.List;

/**
 * 驱动管理 Service 实现
 * <p>
 * 完整实现：JAR 上传（SHA256 + 驱动类探测）、CRUD、引用校验、乐观锁、存储清理。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

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

    @Override
    public DriverUploadResponse uploadDriver(DbType dbType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.endsWith(".jar")) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "仅支持 .jar 文件，zip 解包暂不支持");
        }

        // 计算 SHA256
        String sha256;
        try (InputStream is = file.getInputStream()) {
            sha256 = computeSha256(is);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FAILURE, "计算 SHA256 失败: " + e.getMessage());
        }

        // 内容寻址对象键：driver/{dbType}/{sha256}/{name}.jar
        String objectKey = "driver/" + dbType.name() + "/" + sha256 + "/" + originalName;

        // 存储到 DriverStore（内容寻址，已存在则跳过）
        if (!driverStore.exists(objectKey)) {
            try (InputStream is = file.getInputStream()) {
                driverStore.putObject(objectKey, is, file.getSize());
            } catch (IOException e) {
                throw new BusinessException(ResultCode.FAILURE, "存储驱动 JAR 失败: " + e.getMessage());
            }
        }

        // 获取本地路径用于探测驱动类（LocalDriverStore 直接返回本地路径，MinioDriverStore 下载到临时文件）
        Path localPath = driverStore.downloadToLocal(objectKey);
        List<String> detectedClasses = DriverClassDetector.detectDriverClasses(new Path[]{localPath});
        String driverClass = detectedClasses.isEmpty() ? null : detectedClasses.get(0);

        DriverUploadResponse response = new DriverUploadResponse();
        response.setJarSha256(sha256);
        response.setObjectKey(objectKey);
        response.setFileSize(file.getSize());
        response.setDetectedDriverClasses(detectedClasses);
        response.setDriverClass(driverClass);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDriver(DriverCreateRequest request) {
        // 校验必须先上传 JAR
        if (!StringUtils.hasText(request.getJarSha256()) || !StringUtils.hasText(request.getObjectKey())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "请先上传驱动 JAR 文件");
        }

        Driver driver = driverConverter.toDriver(request);
        driver.setDbType(request.getDbType().name());
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

        // 存储清理：检查是否还有其他驱动记录引用同一个 objectKey，无引用则删除 JAR 文件
        long refCount = driverMapper.selectCount(new LambdaQueryWrapper<Driver>()
            .eq(Driver::getObjectKey, driver.getObjectKey())
            .ne(Driver::getId, id));
        if (refCount == 0) {
            try {
                driverStore.deleteObject(driver.getObjectKey());
            } catch (Exception e) {
                log.warn("删除驱动 JAR 文件失败: objectKey={}, error={}", driver.getObjectKey(), e.getMessage());
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
