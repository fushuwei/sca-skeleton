package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.Reference;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.ReferencedBy;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
@ReferencedBy({
    @Reference(table = "sys_role_permission", column = "permission_id", message = "权限已分配给角色，无法删除"),
    @Reference(table = "sys_tenant_package_permission", column = "permission_id", message = "权限已分配给套餐，无法删除"),
    @Reference(table = "sys_permission", column = "parent_id", message = "请先删除子权限")
})
public class SysPermission extends BaseEntity {

    // ==================== 基本信息 ====================

    /** 父权限 ID */
    private String parentId;

    /** 权限名称 */
    private String name;

    /** 英文名称 */
    private String nameEn;

    /** 类型 */
    private String type;

    /** 权限标识 */
    private String code;

    /** 权限域 */
    private String realm;

    // ==================== 前端信息 ====================

    /** 前端路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 图标 */
    private String icon;

    /** 排序号 */
    private Integer sort;

    // ==================== 显示与状态 ====================

    /** 是否可见 */
    private Integer isVisible;

    /** 是否外链 */
    private Integer isExternal;

    /** 状态 */
    private String status;

    // ==================== 层级与备注 ====================

    /** ID 层级路径 */
    private String treePath;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
