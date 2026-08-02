package io.github.fushuwei.scaskeleton.datasource.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverPageRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverUpdateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverOptionResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverResponse;

import java.util.List;

/**
 * 驱动管理 Service
 *
 * @author Fu Wei
 */
public interface DriverService {

    IPage<DriverResponse> pageDrivers(DriverPageRequest request);

    DriverResponse getDriverById(String id);

    void createDriver(DriverCreateRequest request);

    void updateDriver(DriverUpdateRequest request);

    void deleteDriver(String id);

    void changeStatus(String id, String status);

    List<DriverOptionResponse> listDriverOptions(DbType dbType);
}
