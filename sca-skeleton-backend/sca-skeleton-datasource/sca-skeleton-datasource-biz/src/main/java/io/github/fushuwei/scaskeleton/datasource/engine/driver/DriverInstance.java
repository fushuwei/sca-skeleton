package io.github.fushuwei.scaskeleton.datasource.engine.driver;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 驱动实例持有者。
 * <p>
 * 持有隔离加载的 {@link Driver} 实例与对应的 {@link ChildFirstURLClassLoader}，
 * 维护引用计数，提供 TCCL 切换的连接能力。
 * <p>
 * 硬约束（对应设计方案 v1.3 §3.3）：
 * - 驱动对象以 {@code Driver} 接口持有，禁止 import 具体驱动类（破坏隔离）；
 * - 建连与语句执行整个窗口内「切 TCCL → 执行 → finally 还原」。
 *
 * @author Fu Wei
 */
public class DriverInstance {

    private final String driverRecordId;
    private final Driver driver;
    private final ChildFirstURLClassLoader classLoader;
    private final AtomicInteger refCount = new AtomicInteger(0);

    DriverInstance(String driverRecordId, Driver driver, ChildFirstURLClassLoader classLoader) {
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
     * 使用隔离驱动实例建立连接。
     * <p>
     * 切换 TCCL 到驱动隔离 ClassLoader → {@code driver.connect} → finally 还原 TCCL。
     *
     * @param url   JDBC URL
     * @param props 连接属性
     * @return JDBC 连接
     * @throws SQLException 连接失败
     */
    public Connection connect(String url, Properties props) throws SQLException {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            return driver.connect(url, props);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    /**
     * 在驱动隔离 ClassLoader 上下文内执行操作（用于元数据查询等场景）。
     *
     * @param supplier 操作逻辑
     * @param <T>      返回类型
     * @return 操作结果
     */
    public <T> T executeWithDriverClassLoader(Supplier<T> supplier) {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            return supplier.get();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    ChildFirstURLClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * 关闭 ClassLoader（释放 Jar 句柄）。
     * <p>
     * DriverManager 残留清扫由 {@link DriverLifecycle#unloadDriver} 负责（须宿主 TCCL）。
     */
    void close() {
        try {
            classLoader.close();
        } catch (Exception e) {
            // 关闭失败仅忽略，避免影响卸载流程
        }
    }
}
