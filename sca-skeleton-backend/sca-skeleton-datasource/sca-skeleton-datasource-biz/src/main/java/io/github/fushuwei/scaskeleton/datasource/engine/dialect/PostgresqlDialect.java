package io.github.fushuwei.scaskeleton.datasource.engine.dialect;

import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL 方言。
 * <p>
 * PG 以 schema 为命名空间，catalog 为数据库实例。连接到特定库时 getCatalogs 只返回当前库，
 * 故用 {@code pg_database} 系统表列出所有库。
 *
 * @author Fu Wei
 */
@Component
public class PostgresqlDialect extends AbstractJdbcDialect {

    @Override
    public DbType dbType() {
        return DbType.POSTGRESQL;
    }

    @Override
    protected String urlPrefix() {
        return "jdbc:postgresql://";
    }

    @Override
    public List<String> listDatabases(Connection conn) throws SQLException {
        List<String> databases = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT datname FROM pg_database WHERE datistemplate = false ORDER BY datname")) {
            while (rs.next()) {
                databases.add(rs.getString(1));
            }
        }
        return databases;
    }

    @Override
    public List<String> listTables(Connection conn, String databaseName) throws SQLException {
        // PG 用 schema 而非 catalog，databaseName 传入时忽略（表在当前库的 schema 下）
        return super.listTables(conn, null);
    }

    @Override
    public List<String> listViews(Connection conn, String databaseName) throws SQLException {
        return super.listViews(conn, null);
    }

    @Override
    public List<String> listFunctions(Connection conn, String databaseName) throws SQLException {
        // PG 以当前 schema 为命名空间，避免查出 pg_catalog 内置函数
        return queryFunctions(conn, null, conn.getSchema());
    }

    @Override
    public List<String> listProcedures(Connection conn, String databaseName) throws SQLException {
        return queryProcedures(conn, null, conn.getSchema());
    }

    @Override
    public List<String> listColumns(Connection conn, String databaseName, String tableName) throws SQLException {
        return super.listColumns(conn, null, tableName);
    }
}
