package io.github.fushuwei.scaskeleton.datasource.engine.driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 驱动类探测器（只读文件，不实例化）。
 * <p>
 * 自动识别 JAR 中的 JDBC 驱动主类，两种策略：
 * <ol>
 *   <li>读取 {@code META-INF/services/java.sql.Driver} 文件内容（纯文本读取，不走 ServiceLoader）；</li>
 *   <li>fallback：枚举 jar 内 {@code .class} 条目，{@code Class.forName(name, false, cl)} 仅加载不初始化 +
 *       {@code isAssignableFrom(java.sql.Driver.class)} 判断。</li>
 * </ol>
 * 所有探测入口一律「只加载不初始化」，不触发静态块，不在隔离 CL 中实例化 provider。
 *
 * @author Fu Wei
 */
public final class DriverClassDetector {

    private DriverClassDetector() {
    }

    /**
     * 探测 JAR 文件中的驱动主类候选列表。
     *
     * @param jarPaths JAR 文件本地路径数组
     * @return 驱动类全限定名列表（可能为空）
     */
    public static List<String> detectDriverClasses(Path[] jarPaths) {
        URL[] urls = toUrls(jarPaths);
        try (ChildFirstURLClassLoader cl = new ChildFirstURLClassLoader(urls)) {
            List<String> fromServices = readServicesFile(jarPaths, cl);
            if (!fromServices.isEmpty()) {
                return fromServices;
            }
            return scanJarEntries(jarPaths, cl);
        } catch (IOException e) {
            throw new RuntimeException("关闭探测用 ClassLoader 失败", e);
        }
    }

    /**
     * 策略一：读取 META-INF/services/java.sql.Driver。
     */
    private static List<String> readServicesFile(Path[] jarPaths, ClassLoader cl) {
        List<String> result = new ArrayList<>();
        for (Path jarPath : jarPaths) {
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                JarEntry entry = jarFile.getJarEntry("META-INF/services/java.sql.Driver");
                if (entry == null) {
                    continue;
                }
                try (InputStream is = jarFile.getInputStream(entry);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            if (isDriverClass(line, cl)) {
                                result.add(line);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // 读取单个 jar 失败不影响其他 jar 探测
            }
        }
        return result;
    }

    /**
     * 策略二（fallback）：枚举 jar 条目扫描。
     */
    private static List<String> scanJarEntries(Path[] jarPaths, ClassLoader cl) {
        Set<String> candidates = new LinkedHashSet<>();
        for (Path jarPath : jarPaths) {
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.getName().endsWith(".class")) {
                        continue;
                    }
                    String className = entry.getName().replace('/', '.').replace(".class", "");
                    if (isDriverClass(className, cl)) {
                        candidates.add(className);
                    }
                }
            } catch (IOException e) {
                // 忽略
            }
        }
        return new ArrayList<>(candidates);
    }

    /**
     * loadClass（不初始化）+ isAssignableFrom 判断是否为 java.sql.Driver 子类。
     */
    private static boolean isDriverClass(String className, ClassLoader cl) {
        try {
            Class<?> clazz = Class.forName(className, false, cl);
            return java.sql.Driver.class.isAssignableFrom(clazz);
        } catch (Throwable t) {
            return false;
        }
    }

    private static URL[] toUrls(Path[] paths) {
        URL[] urls = new URL[paths.length];
        for (int i = 0; i < paths.length; i++) {
            try {
                urls[i] = paths[i].toUri().toURL();
            } catch (IOException e) {
                throw new RuntimeException("转换 JAR URL 失败: " + paths[i], e);
            }
        }
        return urls;
    }
}
