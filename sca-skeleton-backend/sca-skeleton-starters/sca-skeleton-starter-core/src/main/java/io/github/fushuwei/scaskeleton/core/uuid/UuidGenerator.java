package io.github.fushuwei.scaskeleton.core.uuid;

import java.util.UUID;

/**
 * UUID 生成器接口
 *
 * <p>所有UUID生成都通过此接口，便于统一管理和切换实现</p>
 *
 * <p>设计原则：</p>
 * <ol>
 *    <li>统一入口：所有业务代码只依赖此接口</li>
 *    <li>可替换性：通过工厂模式切换底层实现</li>
 *    <li>向前兼容：为 JDK 29 原生支持 UUID v7 预留扩展点</li>
 * </ol>
 *
 * @author Fu Wei
 */
public interface UuidGenerator {

    /**
     * 生成 UUID v1（时间戳 + MAC 地址）
     */
    UUID generateV1();

    /**
     * 生成 UUID v3（命名空间 + 名称，MD5）
     */
    UUID generateV3(UuidNamespace namespace, String name);

    /**
     * 生成 UUID v4（随机）
     */
    UUID generateV4();

    /**
     * 生成 UUID v5（命名空间 + 名称，SHA-1）
     */
    UUID generateV5(UuidNamespace namespace, String name);

    /**
     * 生成 UUID v6（可排序时间戳）
     */
    UUID generateV6();

    /**
     * 生成 UUID v7（Unix Epoch 可排序，数据库主键首选）
     */
    UUID generateV7();

    /**
     * 指定版本生成
     */
    default UUID generate(UuidVersion version) {
        return switch (version) {
            case V1 -> generateV1();
            case V4 -> generateV4();
            case V6 -> generateV6();
            case V7 -> generateV7();
            default -> throw new UnsupportedOperationException(
                version + " 需要额外参数，请直接调用 generateV3() 或 generateV5() 方法");
        };
    }
}
