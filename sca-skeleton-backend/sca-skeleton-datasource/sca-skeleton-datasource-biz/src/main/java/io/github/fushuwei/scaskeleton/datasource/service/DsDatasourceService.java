package io.github.fushuwei.scaskeleton.datasource.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourcePageRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceUpdateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.datasource.DatasourceResponse;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;

import java.util.List;

/**
 * 数据源管理 Service
 *
 * @author Fu Wei
 */
public interface DsDatasourceService {

    /**
     * 分页查询数据源列表
     */
    IPage<DatasourceResponse> pageDatasources(DatasourcePageRequest request);

    /**
     * 根据 ID 查询数据源详情
     */
    DatasourceResponse getDatasourceById(String id);

    /**
     * 新增数据源
     */
    void createDatasource(DatasourceCreateRequest request);

    /**
     * 更新数据源
     */
    void updateDatasource(DatasourceUpdateRequest request);

    /**
     * 删除数据源
     */
    void deleteDatasource(String id);

    /**
     * 测试数据源连接
     */
    DatasourceResponse testConnection(String id);

    /**
     * 启用/禁用数据源
     */
    void changeEnabled(String id, Integer enabled);

    /**
     * 获取数据库类型下拉
     */
    List<DbType> listDbTypes();

    /**
     * 查询数据源下的数据库列表
     */
    List<String> listDatabases(String datasourceId);

    /**
     * 查询数据源下的表列表
     */
    List<String> listTables(String datasourceId, String database);

    /**
     * 查询表字段列表
     */
    List<String> listColumns(String datasourceId, String table);
}
