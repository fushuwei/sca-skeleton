package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.Reference;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.ReferencedBy;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
@ReferencedBy({
    @Reference(table = "sys_user_dept", column = "dept_id", message = "部门下存在用户，无法删除"),
    @Reference(table = "sys_dept", column = "parent_id", message = "请先删除子部门")
})
public class SysDept extends BaseEntity {

    // ==================== 基本信息 ====================

    /** 租户 ID */
    private String tenantId;

    /** 上级部门 ID */
    private String parentId;

    /** 部门名称 */
    private String name;

    /** 部门编码 */
    private String code;

    /** 排序号 */
    private Integer sort;

    // ==================== 联系信息 ====================

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    // ==================== 状态与层级 ====================

    /** 状态 */
    private String status;

    /** ID 层级路径 */
    private String treePath;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
