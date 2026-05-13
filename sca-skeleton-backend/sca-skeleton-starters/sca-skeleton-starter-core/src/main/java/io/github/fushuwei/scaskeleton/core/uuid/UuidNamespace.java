package io.github.fushuwei.scaskeleton.core.uuid;

import java.util.UUID;

/**
 * RFC 标准命名空间，用于 v3 / v5 UUID 生成
 *
 * <p>说明：根据 RFC 4122 标准定义的命名空间 UUID，用于生成基于名称的 UUID（v3 和 v5）。
 * 这些命名空间提供了标准的标识符，确保基于相同名称生成的 UUID 具有一致性。
 *
 * @author Fu Wei
 */
public enum UuidNamespace {

    /**
     * DNS 命名空间的 UUID，用于基于 DNS 名称的 UUID 生成
     */
    NAMESPACE_DNS(new UUID(0x6ba7b8109dad11d1L, 0x80b400c04fd430c8L)),

    /**
     * URL 命名空间的 UUID，用于基于 URL 名称的 UUID 生成
     */
    NAMESPACE_URL(new UUID(0x6ba7b8119dad11d1L, 0x80b400c04fd430c8L)),

    /**
     * OID 命名空间的 UUID，用于基于 ASN.1 对象标识符的 UUID 生成
     */
    NAMESPACE_OID(new UUID(0x6ba7b8129dad11d1L, 0x80b400c04fd430c8L)),

    /**
     * X500 命名空间的 UUID，用于基于 X.500 名称的 UUID 生成
     */
    NAMESPACE_X500(new UUID(0x6ba7b8149dad11d1L, 0x80b400c04fd430c8L));


    private final UUID value;

    /**
     * 构造函数，将字符串形式的 UUID 转换为 UUID 对象
     *
     * @param value 命名空间 UUID 的字符串表示
     */
    UuidNamespace(UUID value) {
        this.value = value;
    }

    /**
     * 获取命名空间的 UUID 对象
     *
     * @return 命名空间的 UUID 对象
     */
    public UUID value() {
        return this.value;
    }
}
