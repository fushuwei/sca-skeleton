package io.github.fushuwei.scaskeleton.datasource.api.request.datasource;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据源分页查询请求
 *
 * @author Fu Wei
 */
@Data
public class DatasourcePageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 数据库类型（可选筛选） */
    private String dbType;

    /** 关键字搜索（名称/主机） */
    private String keyword;

    /** 是否启用（1/0） */
    private Integer isEnabled;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;
}
