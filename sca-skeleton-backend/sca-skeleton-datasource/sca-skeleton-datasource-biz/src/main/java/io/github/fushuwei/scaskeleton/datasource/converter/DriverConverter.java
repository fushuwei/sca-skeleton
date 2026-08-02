package io.github.fushuwei.scaskeleton.datasource.converter;

import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverResponse;
import io.github.fushuwei.scaskeleton.datasource.entity.Driver;
import org.mapstruct.Mapper;

/**
 * 驱动对象转换器
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface DriverConverter {

    Driver toDriver(DriverCreateRequest request);

    DriverResponse toDriverResponse(Driver driver);
}
