package io.github.fushuwei.scaskeleton.log.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录日志事件元数据载体类
 *
 * @author Fu Wei
 */
@Data
@NoArgsConstructor
public class LoginLogEvent {

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 登录时输入的用户名（原始输入，无论用户是否存在都记录）
     */
    private String username;

    /**
     * 客户端 IP
     */
    private String clientIp;

    /**
     * 登录位置
     */
    private String location;

    /**
     * 设备类型
     */
    private String device;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 是否成功：0-失败，1-成功
     */
    private Integer isSuccess;

    /**
     * 异常信息（失败时填入）
     */
    private String errorMessage;

    /**
     * 操作耗时（毫秒）
     */
    private Long costMs;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
}
