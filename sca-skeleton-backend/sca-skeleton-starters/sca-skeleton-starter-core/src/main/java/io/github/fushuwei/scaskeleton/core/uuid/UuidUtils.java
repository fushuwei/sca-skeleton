package io.github.fushuwei.scaskeleton.core.uuid;

import java.util.Objects;
import java.util.UUID;

/**
 * UUID 全局统一工具类
 *
 * <p>对外提供静态方法，封装 {@link UuidGeneratorFactory} 的调用细节，业务代码直接使用此工具类即可
 *
 * @author Fu Wei
 */
public final class UuidUtils {

    /**
     * 默认使用 UUID v7
     */
    private static final UuidVersion DEFAULT_VERSION = UuidVersion.V7;

    /**
     * UUID 生成器
     */
    private static final UuidGenerator UUID_GENERATOR = UuidGeneratorFactory.get();

    /**
     * 私有构造方法，防止外部实例化
     *
     * @throws UnsupportedOperationException 始终抛出，禁止实例化此类
     */
    private UuidUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 通用无参生成方法，适用于：v1 / v4 / v6 / v7
     *
     * @param version UUID 版本
     * @return UUID
     */
    public static UUID generate(UuidVersion version) {
        Objects.requireNonNull(version, "version cannot be null");
        return switch (version) {
            case V1 -> UUID_GENERATOR.generateV1();
            case V4 -> UUID_GENERATOR.generateV4();
            case V6 -> UUID_GENERATOR.generateV6();
            case V7 -> UUID_GENERATOR.generateV7();
            case V3, V5 -> throw new IllegalArgumentException(
                version + " requires namespace and name");
        };
    }

    /**
     * 通用无参生成方法，适用于：v1 / v4 / v6 / v7
     *
     * @param version UUID 版本
     * @return UUID 字符串
     */
    public static String generateStr(UuidVersion version) {
        return generate(version).toString();
    }

    /**
     * 通用有参生成方法，适用于：v3 / v5
     *
     * @param version   UUID 版本
     * @param namespace 命名空间
     * @param name      命名
     * @return UUID
     */
    public static UUID generate(UuidVersion version, UuidNamespace namespace, String name) {
        Objects.requireNonNull(version, "version cannot be null");
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        return switch (version) {
            case V3 -> UUID_GENERATOR.generateV3(namespace, name);
            case V5 -> UUID_GENERATOR.generateV5(namespace, name);
            default -> throw new IllegalArgumentException(
                version + " does not accept namespace and name");
        };
    }

    /**
     * 通用有参生成方法，适用于：v3 / v5
     *
     * @param version   UUID 版本
     * @param namespace 命名空间
     * @param name      命名
     * @return UUID 字符串
     */
    public static String generateStr(UuidVersion version, UuidNamespace namespace, String name) {
        return generate(version, namespace, name).toString();
    }

    /**
     * 生成默认版本的 UUID 对象
     *
     * @return UUID 对象
     */
    public static UUID next() {
        return generate(DEFAULT_VERSION);
    }

    /**
     * 生成默认版本的 UUID 字符串
     *
     * @return UUID 字符串
     */
    public static String nextStr() {
        return next().toString();
    }

    /**
     * 生成默认版本的 UUID 简单字符串（不包含 '-'）
     *
     * @return UUID 简单字符串（不包含 '-'）
     */
    public static String nextSimpleStr() {
        return nextStr().replace("-", "");
    }

    /**
     * 生成 UUID v4 对象
     *
     * @return UUID 对象
     */
    public static UUID v4() {
        return UUID_GENERATOR.generateV4();
    }

    /**
     * 生成 UUID v4 字符串
     *
     * @return UUID 字符串
     */
    public static String v4Str() {
        return v4().toString();
    }

    /**
     * 生成 UUID v4 简单字符串（不包含 '-'）
     *
     * @return UUID 简单字符串（不包含 '-'）
     */
    public static String v4SimpleStr() {
        return v4Str().replace("-", "");
    }

    /**
     * 生成 UUID v7 对象
     *
     * @return UUID 对象
     */
    public static UUID v7() {
        return UUID_GENERATOR.generateV7();
    }

    /**
     * 生成 UUID v7 字符串
     *
     * @return UUID 字符串
     */
    public static String v7Str() {
        return v7().toString();
    }

    /**
     * 生成 UUID v7 简单字符串（不包含 '-'）
     *
     * @return UUID 简单字符串（不包含 '-'）
     */
    public static String v7SimpleStr() {
        return v7Str().replace("-", "");
    }
}
