package io.github.fushuwei.scaskeleton.system.api.response.dept;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门响应对象
 *
 * @author Fu Wei
 */
@Data
public class DeptResponse {

    // ==================== 基本信息 ====================

    /** 部门 ID */
    private String id;

    /** 租户 ID */
    private String tenantId;

    /** 上级部门 ID（顶级为 "0"） */
    private String parentId;

    /** 部门名称 */
    private String name;

    /** 部门编码 */
    private String code;

    // ==================== 联系方式 ====================

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    // ==================== 属性信息 ====================

    /** 状态 */
    private String status;

    /** 层级路径 */
    private String treePath;

    /** 排序号 */
    private Integer sort;

    /** 乐观锁版本号 */
    private Integer version;

    // ==================== 审计字段 ====================

    /** 创建人 ID */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新人 ID */
    private String updateBy;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
