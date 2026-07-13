package io.github.fushuwei.scaskeleton.log.event;

import java.time.LocalDateTime;

/**
 * 登录日志事件元数据载体类
 *
 * @author Fu Wei
 */
public record LoginLogEvent(
    /* 租户 ID（失败且用户不存在时为 null） */
    String tenantId,
    /* 用户 ID（同上） */
    String userId,
    /* 登录时输入的用户名（原始输入，无论用户是否存在都记录） */
    String username,
    /* 客户端 IP */
    String clientIp,
    /* 登录位置 */
    String location,
    /* 设备类型 */
    String device,
    /* 浏览器 */
    String browser,
    /* 操作系统 */
    String os,
    /* 是否成功：0-失败，1-成功 */
    Integer isSuccess,
    /* 异常信息（失败时填入） */
    String errorMessage,
    /* 操作耗时（毫秒） */
    Long costMs,
    /* 登录时间 */
    LocalDateTime loginTime
) {
}
