package io.github.fushuwei.scaskeleton.datasource.service;

import io.github.fushuwei.scaskeleton.datasource.api.request.query.SqlExecuteRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.query.SqlExecuteResponse;

/**
 * 数据查询 Service
 *
 * @author Fu Wei
 */
public interface QueryService {

    /**
     * 执行只读 SQL 查询
     *
     * @param request 查询请求
     * @return 查询结果
     */
    SqlExecuteResponse executeSql(SqlExecuteRequest request);
}
