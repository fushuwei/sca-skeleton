package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.Reference;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.ReferencedBy;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
@ReferencedBy({
    @Reference(table = "sys_user_role", column = "role_id", message = "角色已分配给用户，无法删除")
})
public class SysRole extends BaseEntity {

    // ==================== 基本信息 ====================

    /** 租户 ID */
    private String tenantId;

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;

    // ==================== 权限信息 ====================

    /** 数据权限范围 */
    private String dataScope;

    /** 角色域 */
    private String realm;

    /** 是否系统内置 */
    private Integer isBuiltin;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
