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
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverFileResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverOptionResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverResponse;
import io.github.fushuwei.scaskeleton.datasource.converter.DriverConverter;
import io.github.fushuwei.scaskeleton.datasource.engine.storage.DriverStore;
import io.github.fushuwei.scaskeleton.datasource.engine.storage.DriverStoreProperties;
import io.github.fushuwei.scaskeleton.datasource.entity.Driver;
import io.github.fushuwei.scaskeleton.datasource.entity.DriverFile;
import io.github.fushuwei.scaskeleton.datasource.mapper.DriverFileMapper;
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
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 驱动管理 Service 实现。
 * <p>
 * 采用目录式管理：每个驱动记录对应一个目录 {driverName}/，
 * 目录下存放驱动文件 + 依赖 JAR，加载时用一个 URLClassLoader 全部加载。
 * <p>
 * 驱动文件元数据存储在 ds_driver_file 表，一个驱动可关联多个文件。
 * <p>
 * 创建流程：表单字段与驱动文件随同一次 multipart 请求提交 → 校验驱动名称唯一 →
 * 文件直接写入正式目录 {basePath}/{driverName}/ → 落库 + 落文件表。
 * 不再使用临时目录，表单未提交即不产生任何服务端文件，避免脏数据。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverMapper driverMapper;
    private final DriverFileMapper driverFileMapper;
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
        return driverPage.convert(this::toDriverResponseWithFiles);
    }

    @Override
    public DriverResponse getDriverById(String id) {
        Driver driver = loadDriverEntity(id);
        return toDriverResponseWithFiles(driver);
    }

    /**
     * 创建驱动（表单字段 + 驱动文件随同一次 multipart 请求提交）。
     * <p>
     * 校验驱动名称唯一后，文件直接写入正式目录 {basePath}/{driverName}/，落库驱动主表与文件表。
     * 未提交表单时服务端不产生任何文件，不存在脏数据问题。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDriver(DriverCreateRequest request, MultipartFile[] files) {
        // 校验驱动名称唯一（作为目录名必须唯一）
        if (existsByDriverName(request.getDriverName())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "驱动名称已存在: " + request.getDriverName());
        }

        // 校验驱动文件
        List<MultipartFile> validFiles = new ArrayList<>();
        Set<String> fileNames = new HashSet<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String originalName = file.getOriginalFilename();
                if (originalName == null || !originalName.endsWith(".jar")) {
                    throw new BusinessException(ResultCode.VALIDATION_ERROR, "仅支持 .jar 文件: " + originalName);
                }
                if (!fileNames.add(originalName)) {
                    throw new BusinessException(ResultCode.VALIDATION_ERROR, "驱动文件名重复: " + originalName);
                }
                validFiles.add(file);
            }
        }
        if (validFiles.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "请上传驱动文件");
        }

        String driverDir = request.getDriverName();

        // 写入驱动文件（单次流式读取，同步计算 SHA-256）
        List<DriverFile> driverFiles = new ArrayList<>();
        int sortOrder = 0;
        for (MultipartFile file : validFiles) {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new BusinessException(ResultCode.FAILURE, "SHA-256 算法不可用");
            }
            try (InputStream is = file.getInputStream();
                 DigestInputStream dis = new DigestInputStream(is, digest)) {
                driverStore.putJar(driverDir, file.getOriginalFilename(), dis, file.getSize());
            } catch (IOException e) {
                throw new BusinessException(ResultCode.FAILURE, "存储驱动文件失败: " + e.getMessage());
            }

            DriverFile driverFile = new DriverFile();
            driverFile.setFileName(file.getOriginalFilename());
            driverFile.setFileSize(file.getSize());
            driverFile.setSha256(HexFormat.of().formatHex(digest.digest()));
            driverFile.setSortOrder(sortOrder++);
            driverFiles.add(driverFile);
        }

        // 落库驱动主表
        Driver driver = driverConverter.toDriver(request);
        driver.setDbType(request.getDbType().name());
        driver.setObjectKey(driverDir);
        driver.setStatus("enabled");
        driver.setIsBuiltin(0);
        driver.setStorageType(driverStoreProperties.getStorageType());
        driverMapper.insert(driver);

        // 落库驱动文件表（逐个文件记录元数据）
        for (DriverFile driverFile : driverFiles) {
            driverFile.setDriverId(driver.getId());
            driverFileMapper.insert(driverFile);
        }
        log.info("创建驱动成功: driverName={}, fileCount={}", request.getDriverName(), driverFiles.size());
    }

    private boolean existsByDriverName(String driverName) {
        if (!StringUtils.hasText(driverName)) {
            return false;
        }
        return driverMapper.selectCount(new LambdaQueryWrapper<Driver>()
            .eq(Driver::getDriverName, driverName)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDriver(DriverUpdateRequest request) {
        Driver driver = loadDriverEntity(request.getId());

        // 驱动名称变更：校验唯一性 + 重命名存储目录 + 更新 objectKey
        if (StringUtils.hasText(request.getDriverName()) && !request.getDriverName().equals(driver.getDriverName())) {
            if (existsByDriverName(request.getDriverName())) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "驱动名称已存在: " + request.getDriverName());
            }
            driverStore.renameDriverDir(driver.getObjectKey(), request.getDriverName());
            driver.setObjectKey(request.getDriverName());
            driver.setDriverName(request.getDriverName());
        }

        if (request.getDbType() != null) {
            driver.setDbType(request.getDbType().name());
        }
        if (StringUtils.hasText(request.getDriverClass())) {
            driver.setDriverClass(request.getDriverClass());
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

        // 删除驱动文件记录
        driverFileMapper.delete(new LambdaQueryWrapper<DriverFile>()
            .eq(DriverFile::getDriverId, id));
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

        // 删除驱动文件记录
        driverFileMapper.delete(new LambdaQueryWrapper<DriverFile>()
            .in(DriverFile::getDriverId, ids));
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
            .map(d -> new DriverOptionResponse(d.getId(), d.getDriverName()))
            .toList();
    }

    // ── 私有辅助方法 ──

    /**
     * 将 Driver 实体转为响应对象，并附带文件列表与总大小。
     */
    private DriverResponse toDriverResponseWithFiles(Driver driver) {
        DriverResponse response = driverConverter.toDriverResponse(driver);
        List<DriverFile> files = driverFileMapper.selectList(
            new LambdaQueryWrapper<DriverFile>()
                .eq(DriverFile::getDriverId, driver.getId())
                .orderByAsc(DriverFile::getSortOrder));
        List<DriverFileResponse> fileResponses = driverConverter.toDriverFileResponseList(files);
        response.setFiles(fileResponses);
        response.setTotalFileSize(fileResponses.stream()
            .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0L)
            .sum());
        return response;
    }

    private Driver loadDriverEntity(String id) {
        Driver driver = driverMapper.selectById(id);
        if (driver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "驱动不存在");
        }
        return driver;
    }
}
