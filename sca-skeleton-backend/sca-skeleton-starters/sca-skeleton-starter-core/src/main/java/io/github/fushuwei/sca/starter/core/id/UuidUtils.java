package io.github.fushuwei.sca.starter.core.id;

import java.util.UUID;

/**
 * 全局 UUID 生成工具。
 *
 * @author Fu Wei
 */
public final class UuidUtils {

    // 私有构造器用于禁止实例化工具类。
    private UuidUtils() {
    }

    // 生成去除连字符的 32 位小写 UUID 字符串。
    public static String nextSimpleStr() {
        // 获取标准 UUID 字符串。
        String rawUuid = UUID.randomUUID().toString();
        // 去除连字符以满足统一 ID 规则。
        return rawUuid.replace("-", "");
    }
}
