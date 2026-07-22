package io.github.fushuwei.scaskeleton.auth.security;

/**
 * 登录渠道枚举：区分管理后台与前台门户的用户加载策略。
 *
 * @author Fu Wei
 */
public enum LoginChannel {

    /** 管理后台：加载 realm=admin 的用户 */
    ADMIN("admin"),

    /** 前台门户：加载 realm=portal 的用户 */
    PORTAL("portal");

    /** 表单隐藏域 / 请求参数使用的字符串值 */
    private final String value;

    LoginChannel(String value) {
        this.value = value;
    }

    /**
     * @return 渠道字符串值（写入表单 {@code loginChannel}）
     */
    public String getValue() {
        return value;
    }

    /**
     * 从请求参数解析登录渠道；无法识别时默认 admin。
     *
     * @param raw 原始参数值
     * @return 枚举实例
     */
    public static LoginChannel fromValue(String raw) {
        // portal 字符串精确匹配
        if (PORTAL.value.equals(raw)) {
            return PORTAL;
        }
        // 其它情况一律视为 admin，保证后台登录始终可用
        return ADMIN;
    }
}
