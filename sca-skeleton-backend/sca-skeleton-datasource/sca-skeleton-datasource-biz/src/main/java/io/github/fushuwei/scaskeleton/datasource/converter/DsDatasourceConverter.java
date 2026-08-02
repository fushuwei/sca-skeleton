package io.github.fushuwei.scaskeleton.datasource.converter;

import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.datasource.DatasourceResponse;
import io.github.fushuwei.scaskeleton.datasource.entity.DsDatasource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 数据源对象转换器
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface DsDatasourceConverter {

    /**
     * 新增请求转实体
     */
    DsDatasource toDsDatasource(DatasourceCreateRequest request);

    /**
     * 实体转响应
     */
    @Mapping(target = "driverName", ignore = true)
    DatasourceResponse toDatasourceResponse(DsDatasource datasource);
}
