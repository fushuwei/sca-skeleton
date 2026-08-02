package io.github.fushuwei.scaskeleton.datasource.engine.dialect;

import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据库方言接口。
 * <p>
 * 每种数据库一个实现，统一接口，{@link DialectRegistry} 按 {@link DbType} 分发。
 * 对应设计方案 v1.3 §5 方言抽象层。
 *
 * @author Fu Wei
 */
public interface Dialect {

    /** 该方言对应的数据库类型 */
    DbType dbType();

    /**
     * 构建 JDBC URL。
     *
     * @param host             主机地址
     * @param port             端口
     * @param databaseName     数据库名/Schema/实例名
     * @param connectionParams 连接参数（URL query string 格式，如 useSSL=false&serverTimezone=Asia/Shanghai）
     * @return JDBC URL
     */
    String buildJdbcUrl(String host, int port, String databaseName, String connectionParams);

    /** 心跳 SQL（连接探活） */
    String pingSql();

    /**
     * 列出数据源下所有可访问的数据库。
     *
     * @param conn JDBC 连接
     * @return 数据库名列表
     */
    List<String> listDatabases(Connection conn) throws SQLException;

    /**
     * 列出指定数据库下的表。
     *
     * @param conn         JDBC 连接
     * @param databaseName 数据库名（可为 null，表示当前库）
     * @return 表名列表
     */
    List<String> listTables(Connection conn, String databaseName) throws SQLException;

    /**
     * 列出指定表的字段。
     *
     * @param conn         JDBC 连接
     * @param databaseName 数据库名（可为 null）
     * @param tableName    表名
     * @return 字段名列表
     */
    List<String> listColumns(Connection conn, String databaseName, String tableName) throws SQLException;
}
