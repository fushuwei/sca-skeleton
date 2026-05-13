package io.github.fushuwei.scaskeleton.core.uuid.impl;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.fushuwei.scaskeleton.core.uuid.UuidGenerator;
import io.github.fushuwei.scaskeleton.core.uuid.UuidNamespace;

import java.util.UUID;

/**
 * 基于 uuid-creator 库的 UUID 生成器实现
 *
 * <p><b>适用范围：</b>JDK 1.8, 17, 21, 25（原生不支持 UUID v7）
 *
 * @author Fu Wei
 */
public final class UuidCreatorGenerator implements UuidGenerator {

    /**
     * 私有构造方法，防止外部实例化
     */
    private UuidCreatorGenerator() {
    }

    /**
     * 静态内部类持有者
     * <p>
     * 利用类加载机制保证线程安全，且实现懒加载，JVM 在加载外部类时不会加载内部类，
     * 只有调用 {@link #getInstance()} 时才触发类加载
     */
    private static final class Holder {
        private static final UuidCreatorGenerator INSTANCE = new UuidCreatorGenerator();
    }

    /**
     * 获取 {@link UuidCreatorGenerator} 的单例实例
     *
     * @return {@link UuidCreatorGenerator} 唯一实例
     */
    public static UuidCreatorGenerator getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public UUID generateV1() {
        return UuidCreator.getTimeBased();
    }

    @Override
    public UUID generateV3(UuidNamespace namespace, String name) {
        return UuidCreator.getNameBasedMd5(namespace.value(), name);
    }

    @Override
    public UUID generateV4() {
        return UuidCreator.getRandomBased();
    }

    @Override
    public UUID generateV5(UuidNamespace namespace, String name) {
        return UuidCreator.getNameBasedSha1(namespace.value(), name);
    }

    @Override
    public UUID generateV6() {
        return UuidCreator.getTimeOrdered();
    }

    @Override
    public UUID generateV7() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
