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
 * Oracle 方言。
 * <p>
 * Oracle URL 支持两种形态（由 connectionParams 内置键 {@code oracleUrlMode} 标记）：
 * <ul>
 *   <li>服务名（默认）：{@code jdbc:oracle:thin:@//host:port/service}</li>
 *   <li>SID（{@code oracleUrlMode=SID}）：{@code jdbc:oracle:thin:@host:port:SID}</li>
 * </ul>
 * Oracle 没有「数据库」概念（一个实例即一个库），listDatabases 返回实例名。
 *
 * @author Fu Wei
 */
@Component
public class OracleDialect extends AbstractJdbcDialect {

    /** connectionParams 中标记 SID 形态的内置键值对（与前端 Oracle 表单约定一致） */
    private static final String URL_MODE_SID = "oracleUrlMode=SID";

    @Override
    public DbType dbType() {
        return DbType.ORACLE;
    }

    @Override
    protected String urlPrefix() {
        return "jdbc:oracle:thin:@//";
    }

    @Override
    public String buildJdbcUrl(String host, int port, String databaseName, String connectionParams) {
        // SID 形态：jdbc:oracle:thin:@host:port:SID
        if (connectionParams != null && connectionParams.contains(URL_MODE_SID)) {
            StringBuilder url = new StringBuilder("jdbc:oracle:thin:@");
            url.append(host).append(":").append(port);
            if (databaseName != null && !databaseName.isEmpty()) {
                url.append(":").append(databaseName);
            }
            return url.toString();
        }
        // 服务名形态（默认）：//host:port/service
        StringBuilder url = new StringBuilder();
        url.append(urlPrefix()).append(host).append(":").append(port);
        if (databaseName != null && !databaseName.isEmpty()) {
            url.append("/").append(databaseName);
        }
        // Oracle 连接参数一般不用 URL query string
        return url.toString();
    }

    @Override
    public String pingSql() {
        return "SELECT 1 FROM DUAL";
    }

    @Override
    public List<String> listDatabases(Connection conn) throws SQLException {
        // Oracle 一个实例即一个库，返回实例名
        List<String> databases = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM v$database")) {
            while (rs.next()) {
                databases.add(rs.getString(1));
            }
        } catch (SQLException e) {
            // 无权限查询 v$database 时回退到当前用户
            databases.add(conn.getSchema());
        }
        return databases;
    }

    @Override
    public List<String> listTables(Connection conn, String databaseName) throws SQLException {
        // Oracle 用 schema（=用户名）而非 catalog
        return super.listTables(conn, null);
    }

    @Override
    public List<String> listViews(Connection conn, String databaseName) throws SQLException {
        return super.listViews(conn, null);
    }

    @Override
    public List<String> listFunctions(Connection conn, String databaseName) throws SQLException {
        // Oracle 以当前 schema（=用户名）为命名空间，避免查出系统包内置函数
        return queryFunctions(conn, null, conn.getSchema());
    }

    @Override
    public List<String> listProcedures(Connection conn, String databaseName) throws SQLException {
        return queryProcedures(conn, null, conn.getSchema());
    }

    @Override
    public List<String> listSynonyms(Connection conn, String databaseName) throws SQLException {
        // 同义词为 Oracle 特有概念，取当前用户可见的同义词
        List<String> synonyms = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT synonym_name FROM user_synonyms ORDER BY synonym_name")) {
            while (rs.next()) {
                synonyms.add(rs.getString(1));
            }
        } catch (SQLException e) {
            // 无权限访问 user_synonyms 时返回空列表，不影响其他节点浏览
            return new ArrayList<>();
        }
        return synonyms;
    }

    @Override
    public List<String> listColumns(Connection conn, String databaseName, String tableName) throws SQLException {
        return super.listColumns(conn, null, tableName);
    }
}
