package io.github.fushuwei.scaskeleton.core.uuid;

import io.github.fushuwei.scaskeleton.core.uuid.impl.JdkNativeUuidGenerator;
import io.github.fushuwei.scaskeleton.core.uuid.impl.UuidCreatorGenerator;

/**
 * UUID 生成器工厂
 * <p>
 * 通过此类统一管理 UUID 生成器的创建
 *
 * @author Fu Wei
 */
public final class UuidGeneratorFactory {

    /**
     * 私有构造方法，防止外部实例化
     *
     * @throws UnsupportedOperationException 始终抛出，禁止实例化此类
     */
    private UuidGeneratorFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取 UUID 生成器单例实例
     * <p>根据当前运行时 JDK 版本自动选择最优实现：
     * <ul>
     *   <li>JDK 29+：使用 JDK 原生 UUID 生成器 {@link JdkNativeUuidGenerator}</li>
     *   <li>JDK 25 及以下：使用 uuid-creator 库实现 {@link UuidCreatorGenerator}</li>
     * </ul>
     *
     * @return UUID 生成器实例 {@link UuidGenerator}
     */
    public static UuidGenerator get() {
        // 获取运行时 JDK 版本
        Runtime.Version version = Runtime.version();
        int majorVersion = version.feature();

        // JDK 29+ 使用原生实现，否则使用 uuid-creator 库实现
        if (majorVersion >= 29) {
            return JdkNativeUuidGenerator.getInstance();
        } else {
            return UuidCreatorGenerator.getInstance();
        }
    }
}
