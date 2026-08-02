package io.github.fushuwei.scaskeleton.datasource.api.response.datasource;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据库类型选项响应（用于前端下拉选择）
 *
 * @author Fu Wei
 */
@Data
@AllArgsConstructor
public class DbTypeOptionResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 枚举名称（如 MYSQL） */
    private String name;

    /** 显示名称（如 MySQL） */
    private String displayName;

    /** JDBC URL 前缀（如 jdbc:mysql://） */
    private String urlPrefix;

    /** 默认端口 */
    private Integer defaultPort;
}
