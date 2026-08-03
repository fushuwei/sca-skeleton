package io.github.fushuwei.scaskeleton.datasource.converter;

import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverFileResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverResponse;
import io.github.fushuwei.scaskeleton.datasource.entity.Driver;
import io.github.fushuwei.scaskeleton.datasource.entity.DriverFile;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 驱动对象转换器
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface DriverConverter {

    Driver toDriver(DriverCreateRequest request);

    DriverResponse toDriverResponse(Driver driver);

    DriverFileResponse toDriverFileResponse(DriverFile driverFile);

    List<DriverFileResponse> toDriverFileResponseList(List<DriverFile> driverFiles);
}
