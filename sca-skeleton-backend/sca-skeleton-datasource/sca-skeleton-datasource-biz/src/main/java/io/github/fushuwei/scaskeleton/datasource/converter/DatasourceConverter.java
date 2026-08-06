package io.github.fushuwei.scaskeleton.datasource.converter;

import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.datasource.DatasourceResponse;
import io.github.fushuwei.scaskeleton.datasource.entity.Datasource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 数据源对象转换器
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface DatasourceConverter {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "poolConfig", ignore = true)
    Datasource toDatasource(DatasourceCreateRequest request);

    @Mapping(target = "driverName", ignore = true)
    @Mapping(target = "password", ignore = true)
    DatasourceResponse toDatasourceResponse(Datasource datasource);
}
