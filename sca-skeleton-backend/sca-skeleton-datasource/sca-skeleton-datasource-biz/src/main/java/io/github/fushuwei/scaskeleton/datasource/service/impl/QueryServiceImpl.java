package io.github.fushuwei.scaskeleton.datasource.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import io.github.fushuwei.scaskeleton.datasource.api.request.query.SqlExecuteRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.query.SqlExecuteResponse;
import io.github.fushuwei.scaskeleton.datasource.engine.dialect.Dialect;
import io.github.fushuwei.scaskeleton.datasource.engine.dialect.DialectRegistry;
import io.github.fushuwei.scaskeleton.datasource.engine.driver.DriverInstance;
import io.github.fushuwei.scaskeleton.datasource.engine.driver.DriverLifecycle;
import io.github.fushuwei.scaskeleton.datasource.engine.security.CredentialCipher;
import io.github.fushuwei.scaskeleton.datasource.engine.storage.DriverStore;
import io.github.fushuwei.scaskeleton.datasource.entity.Datasource;
import io.github.fushuwei.scaskeleton.datasource.entity.Driver;
import io.github.fushuwei.scaskeleton.datasource.mapper.DatasourceMapper;
import io.github.fushuwei.scaskeleton.datasource.mapper.DriverMapper;
import io.github.fushuwei.scaskeleton.datasource.service.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 数据查询 Service 实现。
 * <p>
 * 对已配置的数据源执行只读 SQL 查询，复用 DatasourceServiceImpl 的驱动加载与连接构建逻辑。
 * <p>
 * 安全约束：
 * - SQL 安全校验（JSqlParser）：仅允许 SELECT / SHOW / DESC / EXPLAIN，拒绝 DML/DDL；
 * - 行数限制：默认 1000，上限 10000；
 * - 主边界为数据库只读账号，关键字黑名单为纵深防御。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private final DatasourceMapper datasourceMapper;
    private final DriverMapper driverMapper;
    private final DriverLifecycle driverLifecycle;
    private final DialectRegistry dialectRegistry;
    private final CredentialCipher credentialCipher;
    private final DriverStore driverStore;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public SqlExecuteResponse executeSql(SqlExecuteRequest request) {
        // SQL 安全校验
        validateSqlSafety(request.getSql());

        Datasource ds = loadDatasourceEntity(request.getDatasourceId());
        Driver driver = loadDriverEntity(ds.getDriverId());
        Dialect dialect = dialectRegistry.get(parseDbType(ds.getDbType()));

        Path[] jarPaths = driverStore.listLocalJars(driver.getName()).toArray(new Path[0]);
        DriverInstance instance = driverLifecycle.acquire(
            driver.getId(), driver.getDriverClass(), jarPaths);

        // 目标数据库优先使用请求参数，未指定则用数据源默认库
        String databaseName = StringUtils.hasText(request.getDatabase())
            ? request.getDatabase() : ds.getDatabaseName();
        String jdbcUrl = dialect.buildJdbcUrl(ds.getHost(), ds.getPort(),
            databaseName, toQueryString(ds.getConnectionParams()));
        Properties props = buildConnectionProps(ds);

        // 最大行数：默认 1000，上限 10000
        int maxRows = (request.getMaxRows() == null || request.getMaxRows() <= 0)
            ? 1000 : Math.min(request.getMaxRows(), 10000);

        long start = System.currentTimeMillis();
        try {
            try (Connection conn = instance.connect(jdbcUrl, props)) {
                if (conn == null) {
                    throw new BusinessException("驱动无法识别 JDBC URL: " + jdbcUrl);
                }
                try (var stmt = conn.createStatement()) {
                    stmt.setMaxRows(maxRows);
                    try (ResultSet rs = stmt.executeQuery(request.getSql())) {
                        SqlExecuteResponse response = buildResponse(rs);
                        response.setCostMs(System.currentTimeMillis() - start);
                        return response;
                    }
                }
            }
        } catch (SQLException e) {
            throw new BusinessException("SQL 执行失败: " + e.getMessage());
        } finally {
            driverLifecycle.release(driver.getId());
        }
    }

    // ============================================================
    // 内部辅助方法
    // ============================================================

    /**
     * SQL 安全校验：仅允许 SELECT / SHOW / DESC / EXPLAIN，拒绝 DML/DDL。
     * <p>
     * 解析失败时 fail-closed：SHOW / DESC / DESCRIBE / EXPLAIN 走前缀白名单兜底，其余一律拒绝。
     */
    private void validateSqlSafety(String sql) {
        String trimmedSql = sql.trim();
        String upperSql = trimmedSql.toUpperCase();

        // 危险关键字黑名单（纵深防御，主边界是数据库只读账号）
        if (upperSql.contains("INTO OUTFILE") || upperSql.contains("LOAD DATA")
            || upperSql.contains("LOAD_FILE") || upperSql.contains("SLEEP(")
            || upperSql.startsWith("COPY ")) {
            throw new BusinessException("SQL 包含不允许的关键字");
        }

        // SHOW / DESC / DESCRIBE / EXPLAIN 走前缀白名单
        // JSqlParser 5.x 虽可解析为独立语句类型，但为兼容各 SHOW 变体，统一按前缀放行
        if (upperSql.startsWith("SHOW ") || upperSql.startsWith("DESC ")
            || upperSql.startsWith("DESCRIBE ") || upperSql.startsWith("EXPLAIN ")) {
            // EXPLAIN ANALYZE 会真实执行 DML，必须拒绝
            if (upperSql.contains("ANALYZE")) {
                throw new BusinessException("不允许 EXPLAIN ANALYZE，会真实执行写操作");
            }
            // SHOW / DESC 基本安全，允许通过
            return;
        }

        try {
            Statement stmt = CCJSqlParserUtil.parse(trimmedSql);

            if (stmt instanceof Select select) {
                // 检查 CTE 中是否有写操作
                if (select.getWithItemsList() != null) {
                    for (WithItem<?> withItem : select.getWithItemsList()) {
                        // CTE 中只允许 SELECT，不允许 INSERT/UPDATE/DELETE
                        // 如果 withItem 的子查询不是纯 SELECT，拒绝
                        String cteBody = withItem.toString().toUpperCase();
                        if (cteBody.contains("INSERT") || cteBody.contains("UPDATE")
                            || cteBody.contains("DELETE") || cteBody.contains("MERGE")
                            || cteBody.contains("DROP") || cteBody.contains("CREATE")
                            || cteBody.contains("ALTER") || cteBody.contains("TRUNCATE")) {
                            throw new BusinessException("CTE 中包含不允许的写操作");
                        }
                    }
                }
                return; // 安全
            }

            // 非 SELECT 语句，拒绝
            throw new BusinessException("仅允许 SELECT 查询语句");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // JSqlParser 解析失败 — fail-closed
            // SHOW / DESC / DESCRIBE / EXPLAIN 已在前缀白名单中放行，此处一律拒绝
            throw new BusinessException("SQL 解析失败，仅允许 SELECT / SHOW / DESC / EXPLAIN 语句");
        }
    }

    /**
     * 构建 SQL 执行响应（列名 + 行数据）。
     */
    private SqlExecuteResponse buildResponse(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<String> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.put(columns.get(i - 1), rs.getObject(i));
            }
            rows.add(row);
        }

        SqlExecuteResponse response = new SqlExecuteResponse();
        response.setColumns(columns);
        response.setRows(rows);
        response.setRowCount(rows.size());
        return response;
    }

    /**
     * 构建连接属性（解密密码）。
     */
    private Properties buildConnectionProps(Datasource ds) {
        Properties props = new Properties();
        props.setProperty("user", ds.getUsername());
        props.setProperty("password", credentialCipher.decrypt(ds.getPassword()));
        return props;
    }

    private Datasource loadDatasourceEntity(String id) {
        Datasource datasource = datasourceMapper.selectById(id);
        if (datasource == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "数据源不存在");
        }
        return datasource;
    }

    private Driver loadDriverEntity(String driverId) {
        if (!StringUtils.hasText(driverId)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "数据源未配置驱动");
        }
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "驱动不存在");
        }
        return driver;
    }

    private DbType parseDbType(String dbType) {
        try {
            return DbType.valueOf(dbType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("未知的数据库类型: " + dbType);
        }
    }

    /**
     * 将 connectionParams 转换为 JDBC URL query string。
     * <p>
     * 兼容两种输入格式：
     * <ul>
     *   <li>JSON：{@code {"useSSL": false, "serverTimezone": "Asia/Shanghai"}} → {@code useSSL=false&serverTimezone=Asia/Shanghai}</li>
     *   <li>已为 query string：{@code useSSL=false&serverTimezone=Asia/Shanghai} → 原样返回</li>
     * </ul>
     */
    private String toQueryString(String connectionParams) {
        if (!StringUtils.hasText(connectionParams)) {
            return null;
        }
        String trimmed = connectionParams.trim();
        if (!trimmed.startsWith("{")) {
            return trimmed;
        }
        try {
            Map<String, Object> params = OBJECT_MAPPER.readValue(trimmed, new TypeReference<>() {});
            if (params.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("解析连接参数 JSON 失败，按原始格式使用: {}", trimmed);
            return trimmed;
        }
    }
}
