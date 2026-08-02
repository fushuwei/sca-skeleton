package io.github.fushuwei.scaskeleton.datasource.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverPageRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverUpdateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverOptionResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverResponse;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;

import java.util.List;

/**
 * 驱动管理 Service
 *
 * @author Fu Wei
 */
public interface DsDriverService {

    /**
     * 分页查询驱动列表
     */
    IPage<DriverResponse> pageDrivers(DriverPageRequest request);

    /**
     * 根据数据源 ID 查询驱动详情
     */
    DriverResponse getDriverById(String id);

    /**
     * 新增驱动
     */
    void createDriver(DriverCreateRequest request);

    /**
     * 更新驱动
     */
    void updateDriver(DriverUpdateRequest request);

    /**
     * 删除驱动
     */
    void deleteDriver(String id);

    /**
     * 启用/禁用驱动
     */
    void changeStatus(String id, String status);

    /**
     * 根据数据库类型查询可用驱动选项
     */
    List<DriverOptionResponse> listDriverOptions(DbType dbType);
}
