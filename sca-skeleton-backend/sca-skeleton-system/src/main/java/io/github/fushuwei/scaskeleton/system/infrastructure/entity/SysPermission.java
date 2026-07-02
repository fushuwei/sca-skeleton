package io.github.fushuwei.scaskeleton.system.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限实体（菜单/目录/按钮三级权限树）。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    /** 父权限ID，顶级为 "0" */
    private String parentId;
    private String name;
    /** 类型：module-模块，folder-目录，menu-菜单，button-按钮 */
    private String type;
    /** 权限标识，如 sys:user:list */
    private String code;
    /** 前端路由地址 */
    private String path;
    /** 前端组件路径 */
    private String component;
    private String icon;
    private Integer sort;
    /** 是否可见：0-否，1-是 */
    private Integer isVisible;
    /** 是否外链：0-否，1-是 */
    private Integer isExternal;
    /** 状态：enabled / disabled */
    private String status;
    /** ID 层级路径，逗号分隔 */
    private String treePath;
    private String remark;

    @Version
    private Integer version;
}
