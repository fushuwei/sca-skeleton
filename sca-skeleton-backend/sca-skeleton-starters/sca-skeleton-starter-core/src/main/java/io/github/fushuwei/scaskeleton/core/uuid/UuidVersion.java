package io.github.fushuwei.scaskeleton.core.uuid;

/**
 * 项目统一支持的 UUID 版本
 *
 * <p>版本说明：</p>
 * <ul>
 *   <li>UUID v1, v4, v6, v7：无参直接生成</li>
 *   <li>UUID v3, v5：需要 namespace 和 name 参数</li>
 *   <li>UUID v2：业务中极少使用，本项目不支持</li>
 *   <li>UUID v8：自定义格式，不是单一固定算法，本项目不支持</li>
 * </ul>
 *
 * @author Fu Wei
 */
public enum UuidVersion {

    /**
     * 基于时间戳 + MAC 地址（Gregorian time）
     */
    V1,

    /**
     * 基于命名空间 + MD5 哈希
     */
    V3,

    /**
     * 随机 UUID（最常用）
     */
    V4,

    /**
     * 基于命名空间 + SHA-1 哈希
     */
    V5,

    /**
     * 基于时间戳（可排序，Gregorian time，V1 的优化版）
     */
    V6,

    /**
     * 基于时间戳（可排序，Unix Epoch，推荐用于数据库主键）
     */
    V7,
}
