package io.github.fushuwei.scaskeleton.datasource.engine.driver;

import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 驱动实例持有者。
 * <p>
 * 持有隔离加载的 {@link Driver} 实例与对应的 {@link URLClassLoader}，
 * 维护引用计数，提供直接建连能力。
 * <p>
 * 硬约束：驱动对象以 {@link Driver} 接口持有，禁止 import 具体驱动类（破坏隔离）；
 * 直接调用 {@code driver.connect(url, props)} 建连，完全绕过 {@link java.sql.DriverManager}。
 *
 * @author Fu Wei
 */
public class DriverInstance {

    private final String driverRecordId;
    private final Driver driver;
    private final URLClassLoader classLoader;
    private final AtomicInteger refCount = new AtomicInteger(0);

    DriverInstance(String driverRecordId, Driver driver, URLClassLoader classLoader) {
        this.driverRecordId = driverRecordId;
        this.driver = driver;
        this.classLoader = classLoader;
    }

    public String getDriverRecordId() {
        return driverRecordId;
    }

    void incrementRef() {
        refCount.incrementAndGet();
    }

    int decrementRef() {
        return refCount.decrementAndGet();
    }

    public int getRefCount() {
        return refCount.get();
    }

    /**
     * 使用驱动实例直接建立连接。
     * <p>
     * 直接调用 {@code driver.connect(url, props)}，不经过 {@link java.sql.DriverManager}，
     * 因此不需要切换 TCCL、不需要清扫 DriverManager 残留。
     *
     * @param url   JDBC URL
     * @param props 连接属性
     * @return JDBC 连接
     * @throws SQLException 连接失败
     */
    public Connection connect(String url, Properties props) throws SQLException {
        return driver.connect(url, props);
    }

    /**
     * 关闭 ClassLoader（释放 Jar 句柄）。
     */
    void close() {
        try {
            classLoader.close();
        } catch (Exception e) {
            // 关闭失败仅忽略，避免影响卸载流程
        }
    }
}
