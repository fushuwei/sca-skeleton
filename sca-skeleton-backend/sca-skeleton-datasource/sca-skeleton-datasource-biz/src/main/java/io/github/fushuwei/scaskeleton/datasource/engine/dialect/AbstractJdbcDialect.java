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
     * <p>
     * 仅返回物理表（TABLE），视图由 {@link #listViews} 单独列出。
     */
    @Override
    public List<String> listTables(Connection conn, String databaseName) throws SQLException {
        return listTableLike(conn, databaseName, null, new String[]{"TABLE"});
    }

    /**
     * 默认实现：用 {@code getMetaData().getTables} 以 VIEW 类型列出视图。
     */
    @Override
    public List<String> listViews(Connection conn, String databaseName) throws SQLException {
        return listTableLike(conn, databaseName, null, new String[]{"VIEW"});
    }

    /**
     * 默认实现：用 {@code getMetaData().getFunctions(catalog, null, "%")} 列出函数。
     */
    @Override
    public List<String> listFunctions(Connection conn, String databaseName) throws SQLException {
        return queryFunctions(conn, databaseName, null);
    }

    /**
     * 默认实现：用 {@code getMetaData().getProcedures(catalog, null, "%")} 列出存储过程。
     */
    @Override
    public List<String> listProcedures(Connection conn, String databaseName) throws SQLException {
        return queryProcedures(conn, databaseName, null);
    }

    /**
     * 默认实现：无同义词概念，返回空列表。Oracle 等方言覆盖此方法。
     */
    @Override
    public List<String> listSynonyms(Connection conn, String databaseName) throws SQLException {
        return new ArrayList<>();
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

    /**
     * 按表类型（TABLE/VIEW 等）列出对象，供 listTables/listViews 复用。
     */
    protected List<String> listTableLike(Connection conn, String catalog, String schema, String[] types) throws SQLException {
        List<String> names = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getTables(catalog, schema, "%", types)) {
            while (rs.next()) {
                names.add(rs.getString("TABLE_NAME"));
            }
        }
        return names;
    }

    /**
     * 按 catalog/schema 维度查询函数名，供子类覆盖时复用。
     */
    protected List<String> queryFunctions(Connection conn, String catalog, String schema) throws SQLException {
        List<String> names = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getFunctions(catalog, schema, "%")) {
            while (rs.next()) {
                names.add(rs.getString("FUNCTION_NAME"));
            }
        }
        return names;
    }

    /**
     * 按 catalog/schema 维度查询存储过程名，供子类覆盖时复用。
     */
    protected List<String> queryProcedures(Connection conn, String catalog, String schema) throws SQLException {
        List<String> names = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getProcedures(catalog, schema, "%")) {
            while (rs.next()) {
                names.add(rs.getString("PROCEDURE_NAME"));
            }
        }
        return names;
    }
}
