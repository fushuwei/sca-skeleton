package io.github.fushuwei.scaskeleton.datasource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverPageRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverUpdateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverOptionResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverResponse;
import io.github.fushuwei.scaskeleton.datasource.converter.DriverConverter;
import io.github.fushuwei.scaskeleton.datasource.entity.Driver;
import io.github.fushuwei.scaskeleton.datasource.mapper.DriverMapper;
import io.github.fushuwei.scaskeleton.datasource.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 驱动管理 Service 实现
 *
 * @author Fu Wei
 */
@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverMapper driverMapper;
    private final DriverConverter driverConverter;

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
        Driver driver = driverMapper.selectById(id);
        if (driver == null) {
            throw new BusinessException("驱动不存在");
        }
        return driverConverter.toDriverResponse(driver);
    }

    @Override
    public void createDriver(DriverCreateRequest request) {
        Driver driver = driverConverter.toDriver(request);
        driver.setId(UuidUtils.nextSimpleStr());
        driver.setStatus("enabled");
        driver.setIsBuiltin(0);
        driver.setStorageType("local");
        driverMapper.insert(driver);
    }

    @Override
    public void updateDriver(DriverUpdateRequest request) {
        Driver driver = driverMapper.selectById(request.getId());
        if (driver == null) {
            throw new BusinessException("驱动不存在");
        }
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
        driverMapper.updateById(driver);
    }

    @Override
    public void deleteDriver(String id) {
        Driver driver = driverMapper.selectById(id);
        if (driver == null) {
            throw new BusinessException("驱动不存在");
        }
        if (driver.getIsBuiltin() == 1) {
            throw new BusinessException("内置驱动不允许删除");
        }
        driverMapper.deleteById(id);
    }

    @Override
    public void changeStatus(String id, String status) {
        Driver driver = driverMapper.selectById(id);
        if (driver == null) {
            throw new BusinessException("驱动不存在");
        }
        driver.setStatus(status);
        driverMapper.updateById(driver);
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
}
