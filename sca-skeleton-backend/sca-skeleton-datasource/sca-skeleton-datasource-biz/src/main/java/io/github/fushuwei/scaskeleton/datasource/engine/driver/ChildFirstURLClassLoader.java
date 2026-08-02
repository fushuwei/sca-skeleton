package io.github.fushuwei.scaskeleton.datasource.engine.driver;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/**
 * Child-first URLClassLoader（子优先 + 父优先白名单）。
 * <p>
 * 实现「child-first + 父优先白名单」变体：JDK 平台包与宿主托管库强制父优先，其余类 child-first，
 * 既保证隔离彻底（同名驱动类互不遮蔽），又避免驱动 jar 内捆绑的 org.slf4j 等宿主同名库遮蔽宿主版本。
 * <p>
 * 关键约束（对应设计方案 v1.3 §3.2）：
 * - {@code findClass} 抛 ClassNotFoundException 后才 fallback 到父加载器，不是返回 null；
 * - {@code loadClass} 用 {@code getClassLoadingLock} 同步，避免并发加载同名类竞态；
 * - {@code getResource/getResources} 收到的是斜杠路径（{@code org/slf4j/...}），
 *   {@code isParentFirst} 需对类名点号与资源名斜杠归一化后再匹配。
 *
 * @author Fu Wei
 */
public class ChildFirstURLClassLoader extends URLClassLoader {

    /** JDK 平台包 + 宿主托管库清单，强制父优先 */
    private static final Set<String> DEFAULT_PARENT_FIRST_PREFIXES = Set.of(
        "java.", "javax.", "jakarta.", "sun.", "com.sun.",
        "org.w3c.", "org.xml.", "org.ietf.",
        "org.slf4j.", "org.apache.logging.log4j.", "org.apache.commons.logging.",
        "org.springframework."
    );

    private final Set<String> parentFirstPrefixes;

    /**
     * 使用默认父优先白名单创建类加载器。
     *
     * @param urls    JAR 文件 URL 数组
     */
    public ChildFirstURLClassLoader(URL[] urls) {
        this(urls, getSystemClassLoader(), DEFAULT_PARENT_FIRST_PREFIXES);
    }

    /**
     * 自定义父加载器与父优先白名单。
     *
     * @param urls                JAR 文件 URL 数组
     * @param parent              父加载器
     * @param parentFirstPrefixes 父优先类名前缀集合
     */
    public ChildFirstURLClassLoader(URL[] urls, ClassLoader parent, Set<String> parentFirstPrefixes) {
        super(urls, parent);
        this.parentFirstPrefixes = parentFirstPrefixes;
    }

    /**
     * 判断类名/资源名是否需要父优先加载。
     * <p>
     * 类名用点号（{@code org.slf4j.Logger}），资源名用斜杠（{@code org/slf4j/impl/StaticLoggerBinder.class}），
     * 这里统一把斜杠转点号后匹配前缀。
     *
     * @param name 类名或资源名
     * @return 命中白名单返回 true
     */
    private boolean isParentFirst(String name) {
        String dotted = name.replace('/', '.');
        return parentFirstPrefixes.stream().anyMatch(dotted::startsWith);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
            if (isParentFirst(name)) {
                return super.loadClass(name, resolve);
            }
            try {
                c = findClass(name);
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            } catch (ClassNotFoundException e) {
                return super.loadClass(name, resolve);
            }
        }
    }

    @Override
    public URL getResource(String name) {
        if (isParentFirst(name)) {
            return super.getResource(name);
        }
        URL own = findResource(name);
        return own != null ? own : super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (isParentFirst(name)) {
            return super.getResources(name);
        }
        List<URL> own = Collections.list(findResources(name));
        if (!own.isEmpty()) {
            return Collections.enumeration(own);
        }
        return super.getResources(name);
    }
}
