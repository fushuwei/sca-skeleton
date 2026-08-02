package io.github.fushuwei.scaskeleton.datasource.converter;

import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverResponse;
import io.github.fushuwei.scaskeleton.datasource.entity.DsDriver;
import org.mapstruct.Mapper;

/**
 * 驱动对象转换器
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface DsDriverConverter {

    /**
     * 新增请求转实体
     */
    DsDriver toDsDriver(DriverCreateRequest request);

    /**
     * 实体转响应
     */
    DriverResponse toDriverResponse(DsDriver driver);
}
