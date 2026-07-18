package io.github.fushuwei.scaskeleton.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志实体类
 *
 * @author Fu Wei
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 基本信息 ====================

    /** 主键 ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 租户 ID */
    private String tenantId;

    /** 租户名称 */
    @TableField(exist = false)
    private String tenantName;

    /** 用户 ID */
    private String userId;

    /** 登录用户名 */
    private String username;

    /** 真实姓名 */
    @TableField(exist = false)
    private String realName;

    // ==================== 客户端信息 ====================

    /** 客户端 IP */
    private String clientIp;

    /** 登录位置 */
    private String location;

    /** 设备类型 */
    private String device;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    // ==================== 结果信息 ====================

    /** 是否成功 */
    private Integer isSuccess;

    /** 异常信息 */
    private String errorMessage;

    /** 操作耗时（毫秒） */
    private Long costMs;

    /** 登录时间 */
    private LocalDateTime loginTime;
}
