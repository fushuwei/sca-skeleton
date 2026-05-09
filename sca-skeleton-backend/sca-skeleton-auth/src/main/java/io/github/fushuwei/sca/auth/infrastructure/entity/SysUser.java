package io.github.fushuwei.sca.auth.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 与 sys_user 表对齐的实体，仅承载认证所需字段。
 *
 * @author Fu Wei
 */
@Data
@TableName("sys_user")
public class SysUser {

    // 主键 UUID。
    @TableId
    private String id;
    // 租户维度隔离键。
    private String tenantId;
    // 登录名。
    private String username;
    // 存储加密口令，兼容 {bcrypt} 前缀。
    private String password;
    // 账号状态，与业务状态机一致。
    private String status;
    // 用户类别，区分前后台。
    private String userCategory;
    // 用户类型，区分超级管理员等。
    private String userType;
    // 逻辑删除标记，0 未删 1 已删。
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
