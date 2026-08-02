package io.github.fushuwei.scaskeleton.datasource.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourcePageRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceUpdateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.datasource.DatasourceResponse;

import java.util.List;

/**
 * 数据源管理 Service
 *
 * @author Fu Wei
 */
public interface DatasourceService {

    IPage<DatasourceResponse> pageDatasources(DatasourcePageRequest request);

    DatasourceResponse getDatasourceById(String id);

    void createDatasource(DatasourceCreateRequest request);

    void updateDatasource(DatasourceUpdateRequest request);

    void deleteDatasource(String id);

    DatasourceResponse testConnection(String id);

    void changeEnabled(String id, Integer enabled);

    List<DbType> listDbTypes();

    List<String> listDatabases(String datasourceId);

    List<String> listTables(String datasourceId, String database);

    List<String> listColumns(String datasourceId, String table);
}
