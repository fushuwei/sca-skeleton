package io.github.fushuwei.scaskeleton.security.constant;

/**
 * 用户类型枚举
 * <p>
 * 用枚举表达互斥的用户身份层级。
 * <ul>
 *   <li>{@link #SUPER_ADMIN} — 平台超级管理员，跨租户最高权限，跳过一切权限校验</li>
 *   <li>{@link #TENANT_ADMIN} — 租户管理员，目前仅为类型标签，权限仍由角色控制</li>
 *   <li>{@link #DEPT_ADMIN} — 部门管理员，目前仅为类型标签，权限仍由角色控制</li>
 *   <li>{@link #NORMAL} — 普通用户，权限由角色控制</li>
 * </ul>
 *
 * @author Fu Wei
 */
public enum UserType {

    /** 平台超级管理员 */
    SUPER_ADMIN,

    /** 租户管理员 */
    TENANT_ADMIN,

    /** 部门管理员 */
    DEPT_ADMIN,

    /** 普通用户 */
    NORMAL;

    /**
     * 安全解析用户类型字符串，无法识别时返回 {@link #NORMAL}
     *
     * @param value 用户类型字符串（可为 null）
     * @return 对应的枚举值
     */
    public static UserType of(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        try {
            return UserType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
