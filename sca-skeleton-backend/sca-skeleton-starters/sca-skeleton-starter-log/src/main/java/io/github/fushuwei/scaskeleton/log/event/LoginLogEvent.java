package io.github.fushuwei.scaskeleton.log.event;

import java.time.LocalDateTime;

/**
 * 登录日志事件：携带登录日志的全部元数据，由业务层采集后发布，由 {@link LoginLogEventListener} 异步消费落库。
 * <p>
 * 本类为纯数据载体，不依赖任何业务模块，可跨服务复用。业务层（如 auth 服务）负责在请求线程内
 * 采集 IP、User-Agent、用户信息等 ThreadLocal 相关数据并封装为本事件后发布。
 *
 * @author Fu Wei
 */
public record LoginLogEvent(
    /** 租户 ID（失败且用户不存在时为 null） */
    String tenantId,
    /** 用户 ID（同上） */
    String userId,
    /** 登录时输入的用户名（原始输入，无论用户是否存在都记录） */
    String username,
    /** 客户端 IP */
    String clientIp,
    /** 登录位置 */
    String location,
    /** 设备类型 */
    String device,
    /** 浏览器 */
    String browser,
    /** 操作系统 */
    String os,
    /** 是否成功：0-失败，1-成功 */
    Integer isSuccess,
    /** 异常信息（失败时填入） */
    String errorMessage,
    /** 操作耗时（毫秒） */
    Long costMs,
    /** 登录时间 */
    LocalDateTime loginTime
) {
}
