package io.github.fushuwei.scaskeleton.datasource.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverPageRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverUpdateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverOptionResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 驱动管理 Service
 *
 * @author Fu Wei
 */
public interface DriverService {

    IPage<DriverResponse> pageDrivers(DriverPageRequest request);

    DriverResponse getDriverById(String id);

    /**
     * 创建驱动（表单字段 + 驱动文件随同一次 multipart 请求提交）。
     */
    void createDriver(DriverCreateRequest request, MultipartFile[] files);

    /**
     * 检查驱动名称是否已存在（驱动名称作为目录名，全局唯一）。
     */
    boolean existsByDriverName(String driverName);

    void updateDriver(DriverUpdateRequest request, MultipartFile[] files);

    void deleteDriver(String id);

    /**
     * 批量删除驱动
     *
     * @param ids 驱动 ID 列表
     */
    void batchDeleteDrivers(List<String> ids);

    List<DriverOptionResponse> listDriverOptions(DbType dbType);
}
