package io.github.fushuwei.scaskeleton.core.uuid.impl;

import io.github.fushuwei.scaskeleton.core.uuid.UuidGenerator;
import io.github.fushuwei.scaskeleton.core.uuid.UuidNamespace;

import java.util.UUID;

/**
 * 基于 JDK 原生 UUID 生成器实现
 *
 * <p><b>适用范围：</b>JDK 29 +（原生支持 UUID v7）
 *
 * @author Fu Wei
 */
public final class JdkNativeUuidGenerator implements UuidGenerator {

    /**
     * 私有构造方法，防止外部实例化
     */
    private JdkNativeUuidGenerator() {
    }

    /**
     * 静态内部类持有者
     * <p>
     * 利用类加载机制保证线程安全，且实现懒加载，JVM 在加载外部类时不会加载内部类，
     * 只有调用 {@link #getInstance()} 时才触发类加载
     */
    private static final class Holder {
        private static final JdkNativeUuidGenerator INSTANCE = new JdkNativeUuidGenerator();
    }

    /**
     * 获取 {@link JdkNativeUuidGenerator} 的单例实例
     *
     * @return {@link JdkNativeUuidGenerator} 唯一实例
     */
    public static JdkNativeUuidGenerator getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public UUID generateV1() {
        throw new UnsupportedOperationException("Not supported yet. Requires JDK 29 or higher.");
    }

    @Override
    public UUID generateV3(UuidNamespace namespace, String name) {
        throw new UnsupportedOperationException("Not supported yet. Requires JDK 29 or higher.");
    }

    @Override
    public UUID generateV4() {
        // 所有 JDK 版本均支持，可直接使用
        return UUID.randomUUID();
    }

    @Override
    public UUID generateV5(UuidNamespace namespace, String name) {
        throw new UnsupportedOperationException("Not supported yet. Requires JDK 29 or higher.");
    }

    @Override
    public UUID generateV6() {
        throw new UnsupportedOperationException("Not supported yet. Requires JDK 29 or higher.");
    }

    @Override
    public UUID generateV7() {
        throw new UnsupportedOperationException("Not supported yet. Requires JDK 29 or higher.");
    }
}
