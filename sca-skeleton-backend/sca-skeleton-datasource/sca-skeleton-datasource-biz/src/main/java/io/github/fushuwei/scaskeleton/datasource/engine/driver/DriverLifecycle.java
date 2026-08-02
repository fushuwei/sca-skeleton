package io.github.fushuwei.scaskeleton.datasource.engine.driver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Path;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 驱动生命周期管理（引用计数 + 主动 close + Metaspace 观测）。
 * <p>
 * 每个 driverRecordId 维护引用计数；被连接池/测试连接引用的加载器持强引用，引用归零时才卸载。
 * <p>
 * 卸载顺序（对应设计方案 v1.3 §3.5）：
 * 清扫 DriverManager 中该隔离 CL 的残留注册（deregisterDriver，须宿主 TCCL）→
 * 引用清零 → 从缓存移除 → URLClassLoader.close()（关 Jar 句柄）。
 * <p>
 * P1 实现期陷阱（对应设计方案 v1.3 §3.3 P1）：
 * {@code DriverManager.drivers()} 会用当前线程 TCCL 做 ServiceLoader 扫描并实例化 provider，
 * 故清扫必须以宿主 TCCL 执行，不能在驱动隔离 TCCL 窗口内。
 *
 * @author Fu Wei
 */
@Slf4j
@Component
public class DriverLifecycle {

    private final ConcurrentHashMap<String, DriverInstance> instances = new ConcurrentHashMap<>();

    /**
     * 获取或加载驱动实例（引用计数 +1）。
     * <p>
     * 若实例已缓存则复用并增加引用计数；否则在隔离 CL 中加载、实例化、清扫 DriverManager 残留后缓存。
     *
     * @param driverRecordId 驱动记录 ID
     * @param driverClass    JDBC Driver 全限定类名
     * @param jarPaths       驱动 JAR 本地路径数组
     * @return 驱动实例
     */
    public DriverInstance acquire(String driverRecordId, String driverClass, Path[] jarPaths) {
        AtomicReference<DriverInstance> resultRef = new AtomicReference<>();
        instances.compute(driverRecordId, (id, existing) -> {
            if (existing != null) {
                existing.incrementRef();
                resultRef.set(existing);
                return existing;
            }
            DriverInstance newInstance = loadDriver(id, driverClass, jarPaths);
            resultRef.set(newInstance);
            return newInstance;
        });
        return resultRef.get();
    }

    /**
     * 释放引用（引用计数 -1，归零时卸载）。
     *
     * @param driverRecordId 驱动记录 ID
     */
    public void release(String driverRecordId) {
        instances.computeIfPresent(driverRecordId, (id, instance) -> {
            int newCount = instance.decrementRef();
            if (newCount <= 0) {
                unloadDriver(instance);
                return null;
            }
            return instance;
        });
    }

    /**
     * 加载驱动（实例化后按 classloader 清扫 DriverManager 注册）。
     * <p>
     * 时序（固定写法，对应设计方案 v1.3 §3.3 P1）：
     * 宿主 TCCL 快照 → 切驱动 TCCL 加载+实例化 → 还原宿主 TCCL →
     * 宿主 TCCL 下清扫 DriverManager 残留。
     */
    private DriverInstance loadDriver(String driverRecordId, String driverClass, Path[] jarPaths) {
        URL[] urls = toUrls(jarPaths);
        ChildFirstURLClassLoader classLoader = new ChildFirstURLClassLoader(urls);

        // 1) 宿主 TCCL 快照当前已注册的 Driver 集合
        Set<Driver> before = new HashSet<>();
        DriverManager.drivers().forEach(before::add);

        // 2) 切驱动 TCCL，加载（不初始化）+ 实例化（触发静态初始化，可能 registerDriver）
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Driver driver;
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            Class<?> clazz = Class.forName(driverClass, false, classLoader);
            driver = (Driver) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            try {
                classLoader.close();
            } catch (Exception ignored) {
                // 忽略关闭异常
            }
            throw new RuntimeException("加载驱动失败: " + driverClass, e);
        } finally {
            // 必须还原 TCCL 再操作 DriverManager（P1 约束）
            Thread.currentThread().setContextClassLoader(original);
        }

        // 3) 宿主 TCCL 下清扫：仅摘除本次静态初始化注册进 DriverManager、且属于该隔离 CL 的驱动
        DriverManager.drivers()
            .filter(d -> !before.contains(d))
            .filter(d -> d.getClass().getClassLoader() == classLoader)
            .forEach(d -> {
                try {
                    DriverManager.deregisterDriver(d);
                } catch (SQLException ignored) {
                    // 忽略 deregister 异常
                }
            });

        DriverInstance instance = new DriverInstance(driverRecordId, driver, classLoader);
        instance.incrementRef();
        log.info("驱动已加载: driverRecordId={}, driverClass={}", driverRecordId, driverClass);
        return instance;
    }

    /**
     * 卸载驱动（防御性兜底清扫 + 关闭 ClassLoader）。
     * <p>
     * 清扫须在宿主 TCCL 下执行（release 由业务线程调用，TCCL 为宿主）。
     */
    private void unloadDriver(DriverInstance instance) {
        try {
            DriverManager.drivers()
                .filter(d -> d.getClass().getClassLoader() == instance.getClassLoader())
                .forEach(d -> {
                    try {
                        DriverManager.deregisterDriver(d);
                    } catch (SQLException ignored) {
                        // 忽略
                    }
                });
        } catch (Exception e) {
            log.warn("清扫 DriverManager 残留失败: {}", e.getMessage());
        }

        instance.close();
        log.info("驱动已卸载: driverRecordId={}", instance.getDriverRecordId());
    }

    private URL[] toUrls(Path[] paths) {
        URL[] urls = new URL[paths.length];
        for (int i = 0; i < paths.length; i++) {
            try {
                urls[i] = paths[i].toUri().toURL();
            } catch (Exception e) {
                throw new RuntimeException("转换 JAR URL 失败: " + paths[i], e);
            }
        }
        return urls;
    }
}
