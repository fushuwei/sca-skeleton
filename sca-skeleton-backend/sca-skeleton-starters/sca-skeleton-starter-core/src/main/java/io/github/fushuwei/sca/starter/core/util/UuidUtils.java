package io.github.fushuwei.sca.starter.core.util;

import java.util.UUID;

/**
 * UUID 工具类。
 * <p>
 * 全系统唯一 ID 统一由此类生成，格式为去掉连字符（{@code -}）的 32 位小写字符串。
 * 禁止直接使用 {@link UUID#randomUUID()}{@code .toString()} 原样入库，
 * 以保证字段长度、查询效率和规范统一。
 *
 * @author Fu Wei
 */
public final class UuidUtils {

    private UuidUtils() {
    }

    /**
     * 生成 32 位小写无连字符 UUID 字符串，适用于数据库主键、业务 ID 等场景。
     *
     * @return 32 位小写 UUID，如 {@code 550e8400e29b41d4a716446655440000}
     */
    public static String nextSimpleStr() {
        // 移除连字符并统一转小写，保证格式固定为 32 位
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }
}
