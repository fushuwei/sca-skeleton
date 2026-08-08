package io.github.fushuwei.scaskeleton.datasource.engine.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.datasource.engine.driver.DriverInstance;
import io.github.fushuwei.scaskeleton.datasource.engine.driver.DriverInstanceDataSource;
import io.github.fushuwei.scaskeleton.datasource.engine.driver.DriverLifecycle;
import io.github.fushuwei.scaskeleton.datasource.entity.Datasource;
import io.github.fushuwei.scaskeleton.datasource.entity.Driver;
import io.github.fushuwei.scaskeleton.datasource.mapper.DriverMapper;
import io.github.fushuwei.scaskeleton.datasource.engine.storage.DriverStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import jakarta.annotation.PreDestroy;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源连接池管理器。
 * <p>
 * 维护每个 Datasource 实例的 HikariCP 连接池。
 * 
 * @author Fu Wei
 */
@Slf4j
@Component
public class DatasourcePoolManager {

    private final ConcurrentHashMap<String, HikariDataSource> pools = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> poolDriverIds = new ConcurrentHashMap<>();
    private final JsonMapper jsonMapper;
    private final DriverLifecycle driverLifecycle;
    private final DriverMapper driverMapper;
    private final DriverStore driverStore;

    public DatasourcePoolManager(JsonMapper jsonMapper, DriverLifecycle driverLifecycle, DriverMapper driverMapper, DriverStore driverStore) {
        this.jsonMapper = jsonMapper;
        this.driverLifecycle = driverLifecycle;
        this.driverMapper = driverMapper;
        this.driverStore = driverStore;
    }

    /**
     * 获取物理连接。
     * 如果连接池不存在，则使用传入的配置初始化。
     */
    public Connection getConnection(Datasource ds, String jdbcUrl, Properties props) throws SQLException {
        HikariDataSource dataSource = pools.computeIfAbsent(ds.getId(), id -> {
            log.info("初始化数据源连接池: datasourceId={}", id);
            return createPool(ds, jdbcUrl, props);
        });
        return dataSource.getConnection();
    }

    /**
     * 销毁并移除指定的连接池。
     * 在数据源被更新或删除时调用。
     */
    public void evictPool(String datasourceId) {
        HikariDataSource dataSource = pools.remove(datasourceId);
        if (dataSource != null) {
            log.info("销毁数据源连接池: datasourceId={}", datasourceId);
            dataSource.close();
            String driverId = poolDriverIds.remove(datasourceId);
            if (driverId != null) {
                driverLifecycle.release(driverId);
            }
        }
    }

    /**
     * 关闭所有连接池。
     */
    @PreDestroy
    public void destroyAll() {
        pools.keySet().forEach(this::evictPool);
        log.info("已关闭所有动态数据源连接池");
    }

    private HikariDataSource createPool(Datasource ds, String jdbcUrl, Properties props) {
        Driver driver = driverMapper.selectById(ds.getDriverId());
        if (driver == null) {
            throw new BusinessException("驱动不存在");
        }
        Path[] jarPaths = driverStore.listLocalJars(driver.getName()).toArray(new Path[0]);
        // 获取驱动实例，增加引用计数，确保连接池存活期间驱动不被卸载
        DriverInstance driverInstance = driverLifecycle.acquire(driver.getId(), driver.getDriverClass(), jarPaths);
        poolDriverIds.put(ds.getId(), driver.getId());

        HikariConfig config = new HikariConfig();
        config.setPoolName("HikariPool-" + ds.getId());
        
        // 使用包装后的 DataSource 桥接我们的隔离 Driver
        DriverInstanceDataSource customDataSource = new DriverInstanceDataSource(driverInstance, jdbcUrl, props);
        config.setDataSource(customDataSource);
        
        // 解析并应用连接池配置
        if (StringUtils.hasText(ds.getPoolConfig())) {
            try {
                Map<String, Object> poolConfig = jsonMapper.readValue(ds.getPoolConfig(), new TypeReference<>() {});
                if (poolConfig.containsKey("maximumPoolSize")) {
                    config.setMaximumPoolSize(((Number) poolConfig.get("maximumPoolSize")).intValue());
                }
                if (poolConfig.containsKey("minimumIdle")) {
                    config.setMinimumIdle(((Number) poolConfig.get("minimumIdle")).intValue());
                }
                if (poolConfig.containsKey("connectionTimeout")) {
                    config.setConnectionTimeout(((Number) poolConfig.get("connectionTimeout")).longValue());
                }
                if (poolConfig.containsKey("idleTimeout")) {
                    config.setIdleTimeout(((Number) poolConfig.get("idleTimeout")).longValue());
                }
                if (poolConfig.containsKey("maxLifetime")) {
                    config.setMaxLifetime(((Number) poolConfig.get("maxLifetime")).longValue());
                }
            } catch (Exception e) {
                log.warn("解析数据源连接池配置失败: datasourceId={}, error={}", ds.getId(), e.getMessage());
            }
        }
        
        try {
            return new HikariDataSource(config);
        } catch (Exception e) {
            poolDriverIds.remove(ds.getId());
            driverLifecycle.release(driver.getId());
            throw new BusinessException("创建数据源连接池失败: " + e.getMessage());
        }
    }
}
