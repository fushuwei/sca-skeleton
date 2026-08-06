package io.github.fushuwei.scaskeleton.datasource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourcePageRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceTestConfigRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceUpdateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.datasource.DatasourceResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.datasource.DbTypeOptionResponse;
import io.github.fushuwei.scaskeleton.datasource.converter.DatasourceConverter;
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
import io.github.fushuwei.scaskeleton.datasource.service.DatasourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据源管理 Service 实现。
 * <p>
 * 完整实现：CRUD（凭据加密 + 乐观锁 + 驱动一致性校验）、测试连接（直连 + 状态更新）、
 * 元数据浏览（库/表/字段列表）。
 * <p>
 * 设计约束（对应设计方案 v1.3）：
 * - testConnection 用纯 Driver 实例直连一次（不建池），返回连通性 + 状态更新（§4）；
 * - 元数据浏览复用 DriverLifecycle 引用计数，连接后即释放（§3.5）；
 * - 凭据 AES-GCM 加密入库，密钥版本前缀（§6）；
 * - 数据源与驱动一致性校验 dbType 匹配 + 驱动 enabled（§8 P2-4）。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatasourceServiceImpl implements DatasourceService {

    private final DatasourceMapper datasourceMapper;
    private final DriverMapper driverMapper;
    private final DatasourceConverter datasourceConverter;
    private final DriverLifecycle driverLifecycle;
    private final DialectRegistry dialectRegistry;
    private final CredentialCipher credentialCipher;
    private final DriverStore driverStore;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 连接池配置白名单（HikariCP 核心可调参数）。
     * <p>
     * 前端以固定 key/value 表单提交，仅允许白名单内的 key，
     * 后端校验后序列化为 JSON 存储，避免用户手写 JSON 出错。
     */
    private static final Set<String> POOL_CONFIG_ALLOWED_KEYS = Set.of(
        "maximumPoolSize", "minimumIdle", "connectionTimeout", "idleTimeout", "maxLifetime");

    // ============================================================
    // CRUD
    // ============================================================

    @Override
    public IPage<DatasourceResponse> pageDatasources(DatasourcePageRequest request) {
        Page<Datasource> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Datasource> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getDbType())) {
            wrapper.eq(Datasource::getDbType, request.getDbType());
        }
        if (request.getIsEnabled() != null) {
            wrapper.eq(Datasource::getIsEnabled, request.getIsEnabled());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Datasource::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(Datasource::getName, request.getKeyword())
                .or().like(Datasource::getHost, request.getKeyword()));
        }
        wrapper.orderByDesc(Datasource::getCreateTime);

        IPage<Datasource> dsPage = datasourceMapper.selectPage(page, wrapper);

        // 批量查询 driverName，避免 N+1
        Set<String> driverIds = dsPage.getRecords().stream()
            .map(Datasource::getDriverId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<String, String> driverNameMap = driverIds.isEmpty()
            ? Collections.emptyMap()
            : driverMapper.selectBatchIds(driverIds).stream()
                .collect(Collectors.toMap(Driver::getId, Driver::getName));

        return dsPage.convert(ds -> {
            DatasourceResponse resp = datasourceConverter.toDatasourceResponse(ds);
            resp.setDriverName(driverNameMap.get(ds.getDriverId()));
            return resp;
        });
    }

    @Override
    public DatasourceResponse getDatasourceById(String id) {
        Datasource ds = loadDatasourceEntity(id);
        DatasourceResponse response = datasourceConverter.toDatasourceResponse(ds);
        if (StringUtils.hasText(ds.getDriverId())) {
            Driver driver = driverMapper.selectById(ds.getDriverId());
            if (driver != null) {
                response.setDriverName(driver.getName());
            }
        }
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDatasource(DatasourceCreateRequest request) {
        // 驱动一致性校验
        validateDriverConsistency(request.getDriverId(), request.getDbType());

        Datasource datasource = datasourceConverter.toDatasource(request);
        datasource.setId(UuidUtils.nextSimpleStr());
        datasource.setDbType(request.getDbType().name());
        datasource.setIsEnabled(1);
        datasource.setStatus("offline");
        // AES-GCM 加密凭据
        datasource.setPassword(credentialCipher.encrypt(request.getPassword()));
        datasource.setCipherVersion(credentialCipher.currentCipherVersion());
        datasource.setRemark(request.getRemark());
        datasource.setPoolConfig(serializePoolConfig(request.getPoolConfig()));
        datasourceMapper.insert(datasource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDatasource(DatasourceUpdateRequest request) {
        Datasource datasource = loadDatasourceEntity(request.getId());

        // 数据源类型创建后不可变更：驱动一致性校验一律以库内记录的 dbType 为准
        DbType effectiveDbType = DbType.valueOf(datasource.getDbType());
        validateDriverConsistency(request.getDriverId(), effectiveDbType);

        if (StringUtils.hasText(request.getName())) {
            datasource.setName(request.getName());
        }
        if (StringUtils.hasText(request.getDriverId())) {
            datasource.setDriverId(request.getDriverId());
        }
        if (StringUtils.hasText(request.getHost())) {
            datasource.setHost(request.getHost());
        }
        if (request.getPort() != null) {
            datasource.setPort(request.getPort());
        }
        if (StringUtils.hasText(request.getDatabaseName())) {
            datasource.setDatabaseName(request.getDatabaseName());
        }
        if (StringUtils.hasText(request.getUsername())) {
            datasource.setUsername(request.getUsername());
        }
        if (StringUtils.hasText(request.getPassword())) {
            datasource.setPassword(credentialCipher.encrypt(request.getPassword()));
            datasource.setCipherVersion(credentialCipher.currentCipherVersion());
        }
        if (request.getRemark() != null) {
            datasource.setRemark(request.getRemark());
        }
        if (StringUtils.hasText(request.getConnectionParams())) {
            datasource.setConnectionParams(request.getConnectionParams());
        }
        if (request.getPoolConfig() != null && !request.getPoolConfig().isEmpty()) {
            datasource.setPoolConfig(serializePoolConfig(request.getPoolConfig()));
        }
        datasource.setVersion(request.getVersion());

        int affectedRows = datasourceMapper.updateById(datasource);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDatasource(String id) {
        loadDatasourceEntity(id);
        // TODO M3: 关闭连接池（DataSourcePoolManager）后删除
        datasourceMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDatasources(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // 批量加载并校验存在
        List<Datasource> datasources = datasourceMapper.selectBatchIds(ids);
        if (datasources.isEmpty()) {
            return;
        }
        // TODO M3: 关闭连接池（DataSourcePoolManager）后删除
        datasourceMapper.deleteBatchIds(ids);
    }

    @Override
    public void changeEnabled(String id, Integer enabled) {
        Datasource datasource = loadDatasourceEntity(id);
        datasource.setIsEnabled(enabled);
        int affectedRows = datasourceMapper.updateById(datasource);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }
    }

    @Override
    public List<DbTypeOptionResponse> listDbTypes() {
        return Arrays.stream(DbType.values())
            .map(dbType -> new DbTypeOptionResponse(
                dbType.name(),
                dbType.getDisplayName(),
                dbType.getUrlPrefix(),
                dbType.getDefaultPort()))
            .toList();
    }

    // ============================================================
    // 测试连接（纯 Driver 直连，不建池）
    // ============================================================

    @Override
    public DatasourceResponse testConnection(String id) {
        Datasource ds = loadDatasourceEntity(id);
        Driver driver = loadDriverEntity(ds.getDriverId());
        Dialect dialect = dialectRegistry.get(parseDbType(ds.getDbType()));

        // 加载驱动实例
        DriverInstance instance;
        try {
            Path[] jarPaths = driverStore.listLocalJars(driver.getName()).toArray(new Path[0]);
            instance = driverLifecycle.acquire(driver.getId(), driver.getDriverClass(), jarPaths);
        } catch (Exception e) {
            log.warn("加载驱动失败: datasourceId={}, error={}", id, e.getMessage());
            updateStatus(ds, "error", "加载驱动失败: " + e.getMessage());
            throw new BusinessException("加载驱动失败: " + e.getMessage());
        }

        String jdbcUrl = dialect.buildJdbcUrl(ds.getHost(), ds.getPort(),
            ds.getDatabaseName(), toQueryString(ds.getConnectionParams()));
        Properties props = buildConnectionProps(ds);

        try {
            try (Connection conn = instance.connect(jdbcUrl, props)) {
                if (conn == null) {
                    throw new BusinessException("驱动无法识别 JDBC URL: " + jdbcUrl);
                }
                // 执行心跳 SQL
                try (var stmt = conn.createStatement()) {
                    stmt.execute(dialect.pingSql());
                }
            }
            // 连接成功：更新运行态
            updateStatus(ds, "normal", null);
            log.info("测试连接成功: datasourceId={}, jdbcUrl={}", id, jdbcUrl);
            return buildResponse(ds, driver);
        } catch (Exception e) {
            log.warn("测试连接失败: datasourceId={}, error={}", id, e.getMessage());
            updateStatus(ds, "error", e.getMessage());
            throw new BusinessException("连接失败: " + e.getMessage());
        } finally {
            driverLifecycle.release(driver.getId());
        }
    }

    @Override
    public void testConnectionByConfig(DatasourceTestConfigRequest request) {
        // 驱动一致性校验 + 加载驱动与方言
        validateDriverConsistency(request.getDriverId(), request.getDbType());
        Driver driver = loadDriverEntity(request.getDriverId());
        Dialect dialect = dialectRegistry.get(request.getDbType());

        // 密码：优先用表单输入；编辑模式留空时回退库内已保存的密码
        String password = request.getPassword();
        if (!StringUtils.hasText(password)) {
            if (!StringUtils.hasText(request.getId())) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "密码不能为空");
            }
            Datasource stored = loadDatasourceEntity(request.getId());
            password = credentialCipher.decrypt(stored.getPassword());
        }

        // 加载驱动实例（不落库、不更新状态）
        DriverInstance instance;
        try {
            Path[] jarPaths = driverStore.listLocalJars(driver.getName()).toArray(new Path[0]);
            instance = driverLifecycle.acquire(driver.getId(), driver.getDriverClass(), jarPaths);
        } catch (Exception e) {
            log.warn("加载驱动失败: driverId={}, error={}", driver.getId(), e.getMessage());
            throw new BusinessException("加载驱动失败: " + e.getMessage());
        }

        String jdbcUrl = dialect.buildJdbcUrl(request.getHost(), request.getPort(),
            request.getDatabaseName(), toQueryString(request.getConnectionParams()));
        Properties props = new Properties();
        props.setProperty("user", request.getUsername());
        props.setProperty("password", password);

        try {
            try (Connection conn = instance.connect(jdbcUrl, props)) {
                if (conn == null) {
                    throw new BusinessException("驱动无法识别 JDBC URL: " + jdbcUrl);
                }
                // 执行心跳 SQL
                try (var stmt = conn.createStatement()) {
                    stmt.execute(dialect.pingSql());
                }
            }
            log.info("测试连接配置成功: jdbcUrl={}", jdbcUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("测试连接配置失败: jdbcUrl={}, error={}", jdbcUrl, e.getMessage());
            throw new BusinessException("连接失败: " + e.getMessage());
        } finally {
            driverLifecycle.release(driver.getId());
        }
    }

    // ============================================================
    // 元数据浏览（库/表/字段）
    // ============================================================

    @Override
    public List<String> listDatabases(String datasourceId) {
        return executeQuery(datasourceId, (conn, dialect, ds) -> dialect.listDatabases(conn));
    }

    @Override
    public List<String> listTables(String datasourceId, String database) {
        return executeQuery(datasourceId, (conn, dialect, ds) ->
            dialect.listTables(conn, StringUtils.hasText(database) ? database : null));
    }

    @Override
    public List<String> listColumns(String datasourceId, String database, String table) {
        return executeQuery(datasourceId, (conn, dialect, ds) ->
            dialect.listColumns(conn,
                StringUtils.hasText(database) ? database : ds.getDatabaseName(),
                table));
    }

    // ============================================================
    // 内部辅助方法
    // ============================================================

    /**
     * 元数据查询通用模板：加载驱动 → 建连 → 执行查询 → 释放驱动。
     */
    private <T> T executeQuery(String datasourceId, SqlAction<T> action) {
        Datasource ds = loadDatasourceEntity(datasourceId);
        Driver driver = loadDriverEntity(ds.getDriverId());
        Dialect dialect = dialectRegistry.get(parseDbType(ds.getDbType()));

        Path[] jarPaths = driverStore.listLocalJars(driver.getName()).toArray(new Path[0]);
        DriverInstance instance = driverLifecycle.acquire(
            driver.getId(), driver.getDriverClass(), jarPaths);

        String jdbcUrl = dialect.buildJdbcUrl(ds.getHost(), ds.getPort(),
            ds.getDatabaseName(), toQueryString(ds.getConnectionParams()));
        Properties props = buildConnectionProps(ds);

        try {
            try (Connection conn = instance.connect(jdbcUrl, props)) {
                if (conn == null) {
                    throw new BusinessException("驱动无法识别 JDBC URL: " + jdbcUrl);
                }
                return action.execute(conn, dialect, ds);
            }
        } catch (SQLException e) {
            throw new BusinessException("数据库操作失败: " + e.getMessage());
        } finally {
            driverLifecycle.release(driver.getId());
        }
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

    /**
     * 更新数据源状态（best-effort，不掩盖原始异常）。
     */
    private void updateStatus(Datasource ds, String status, String errorMsg) {
        ds.setStatus(status);
        if ("normal".equals(status)) {
            ds.setErrorMsg(null);
        } else {
            ds.setErrorMsg(StringUtils.hasText(errorMsg) ? errorMsg : "未知错误");
        }
        try {
            datasourceMapper.updateById(ds);
        } catch (Exception e) {
            log.warn("更新数据源连接状态失败: id={}, error={}", ds.getId(), e.getMessage());
        }
    }

    /**
     * 构建响应（填充 driverName）。
     */
    private DatasourceResponse buildResponse(Datasource ds, Driver driver) {
        DatasourceResponse response = datasourceConverter.toDatasourceResponse(ds);
        if (driver != null) {
            response.setDriverName(driver.getName());
        }
        return response;
    }

    /**
     * 校验数据源与驱动一致性（dbType 匹配 + 驱动启用）。
     */
    private void validateDriverConsistency(String driverId, DbType dbType) {
        if (!StringUtils.hasText(driverId)) {
            return;
        }
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "驱动不存在");
        }
        if (!driver.getDbType().equals(dbType.name())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "驱动数据库类型与数据源类型不一致");
        }
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

    /**
     * 连接池配置白名单校验 + JSON 序列化。
     * <p>
     * 前端以固定 key/value 表单提交结构化配置，本方法负责：
     * <ol>
     *   <li>key 白名单校验（仅允许 HikariCP 核心可调参数）；</li>
     *   <li>value 正整数校验（个数与毫秒时长均为正整数）；</li>
     *   <li>序列化为 JSON 字符串存储。</li>
     * </ol>
     *
     * @return JSON 字符串；入参为 null/空时返回 null（使用连接池默认值）
     */
    private String serializePoolConfig(Map<String, Object> poolConfig) {
        if (poolConfig == null || poolConfig.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, Object> entry : poolConfig.entrySet()) {
            String key = entry.getKey();
            if (!POOL_CONFIG_ALLOWED_KEYS.contains(key)) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "不支持的连接池配置项: " + key);
            }
            Object value = entry.getValue();
            if (!(value instanceof Number number) || number.longValue() <= 0) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "连接池配置项 " + key + " 需为正整数");
            }
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(poolConfig);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "连接池配置序列化失败");
        }
    }

    /** SQL 操作函数式接口 */
    @FunctionalInterface
    private interface SqlAction<T> {
        T execute(Connection conn, Dialect dialect, Datasource ds) throws SQLException;
    }
}
