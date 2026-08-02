package io.github.fushuwei.scaskeleton.datasource.engine.driver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Driver;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 驱动生命周期管理（引用计数 + 主动 close）。
 * <p>
 * 采用 Chat2DB 风格方案：每个驱动记录用一个独立的普通 {@link URLClassLoader} 加载其全部 JAR
 * （驱动主 JAR + 所有依赖 JAR 放在同一目录），通过「自包含依赖 + 标准 parent-first 双亲委派」
 * 实现驱动间类隔离。父加载器为宿主 AppClassLoader，但因驱动目录自包含所有依赖，
 * 不会与宿主第三方库产生冲突。
 * <p>
 * 不需要 TCCL 切换、不需要清扫 DriverManager 残留：驱动类以 {@link Driver} 接口持有，
 * 直接调用 {@code driver.connect(url, props)} 建连，完全绕过 {@link java.sql.DriverManager}。
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
     * 若实例已缓存则复用并增加引用计数；否则用普通 {@link URLClassLoader} 加载驱动目录下所有 JAR，
     * {@code Class.forName(driverClass, true, cl)} 触发静态初始化后实例化驱动对象并缓存。
     *
     * @param driverRecordId 驱动记录 ID
     * @param driverClass    JDBC Driver 全限定类名
     * @param jarPaths       驱动目录下所有 JAR 的本地路径数组（驱动主 JAR + 依赖 JAR）
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
     * 加载驱动：普通 URLClassLoader + Class.forName + newInstance。
     * <p>
     * 驱动静态块可能调用 {@code DriverManager.registerDriver} 把自己注册进全局 DriverManager，
     * 但我们完全不用 DriverManager 建连，注册残留不影响功能。
     * 引用归零时 {@link URLClassLoader#close()} 会释放 JAR 文件句柄，
     * DriverManager 对驱动的弱引用不会阻止 ClassLoader 被 GC。
     */
    private DriverInstance loadDriver(String driverRecordId, String driverClass, Path[] jarPaths) {
        URL[] urls = toUrls(jarPaths);
        URLClassLoader classLoader = new URLClassLoader(urls);

        try {
            // Class.forName(driverClass, true, cl) 加载 + 初始化（触发静态块）
            Class<?> clazz = Class.forName(driverClass, true, classLoader);
            Driver driver = (Driver) clazz.getDeclaredConstructor().newInstance();

            DriverInstance instance = new DriverInstance(driverRecordId, driver, classLoader);
            instance.incrementRef();
            log.info("驱动已加载: driverRecordId={}, driverClass={}, jarCount={}",
                driverRecordId, driverClass, jarPaths.length);
            return instance;
        } catch (Exception e) {
            try {
                classLoader.close();
            } catch (Exception ignored) {
                // 忽略关闭异常
            }
            throw new RuntimeException("加载驱动失败: " + driverClass, e);
        }
    }

    /**
     * 卸载驱动：关闭 ClassLoader 释放 JAR 文件句柄。
     */
    private void unloadDriver(DriverInstance instance) {
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
