package io.github.fushuwei.scaskeleton.datasource.engine.dialect;

import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC 方言通用基类。
 * <p>
 * 提供基于 {@link DatabaseMetaData} 的默认元数据查询实现，子类可按需覆盖。
 *
 * @author Fu Wei
 */
public abstract class AbstractJdbcDialect implements Dialect {

    /** 子类提供 URL 前缀（如 jdbc:mysql://） */
    protected abstract String urlPrefix();

    @Override
    public String buildJdbcUrl(String host, int port, String databaseName, String connectionParams) {
        StringBuilder url = new StringBuilder();
        url.append(urlPrefix()).append(host).append(":").append(port);
        if (StringUtils.hasText(databaseName)) {
            url.append("/").append(databaseName);
        }
        if (StringUtils.hasText(connectionParams)) {
            url.append("?").append(connectionParams);
        }
        return url.toString();
    }

    @Override
    public String pingSql() {
        return "SELECT 1";
    }

    /**
     * 默认实现：用 {@code getMetaData().getCatalogs()} 列出数据库。
     * MySQL 等以 catalog 为数据库的适用此实现；PG/Oracle 需覆盖。
     */
    @Override
    public List<String> listDatabases(Connection conn) throws SQLException {
        List<String> databases = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getCatalogs()) {
            while (rs.next()) {
                databases.add(rs.getString("TABLE_CAT"));
            }
        }
        return databases;
    }

    /**
     * 默认实现：用 {@code getMetaData().getTables(catalog, null, "%", types)} 列出表。
     */
    @Override
    public List<String> listTables(Connection conn, String databaseName) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getTables(databaseName, null, "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    /**
     * 默认实现：用 {@code getMetaData().getColumns(catalog, null, table, "%")} 列出字段。
     */
    @Override
    public List<String> listColumns(Connection conn, String databaseName, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getColumns(databaseName, null, tableName, "%")) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }
}
