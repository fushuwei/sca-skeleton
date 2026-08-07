package io.github.fushuwei.scaskeleton.datasource.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourcePageRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceTestConfigRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceUpdateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.datasource.DatasourceResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.datasource.DbTypeOptionResponse;

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

    /**
     * 批量删除数据源
     *
     * @param ids 数据源 ID 列表
     */
    void batchDeleteDatasources(List<String> ids);

    DatasourceResponse testConnection(String id);

    /**
     * 测试未保存的连接配置（表单内「测试连接」）：不落库、不更新状态。
     *
     * @param request 连接配置（编辑模式密码留空时回退库内密码）
     */
    void testConnectionByConfig(DatasourceTestConfigRequest request);

    void changeEnabled(String id, Integer enabled);

    List<DbTypeOptionResponse> listDbTypes();

    List<String> listDatabases(String datasourceId);

    List<String> listTables(String datasourceId, String database);

    List<String> listViews(String datasourceId, String database);

    List<String> listFunctions(String datasourceId, String database);

    List<String> listProcedures(String datasourceId, String database);

    List<String> listSynonyms(String datasourceId, String database);

    List<String> listColumns(String datasourceId, String database, String table);
}
