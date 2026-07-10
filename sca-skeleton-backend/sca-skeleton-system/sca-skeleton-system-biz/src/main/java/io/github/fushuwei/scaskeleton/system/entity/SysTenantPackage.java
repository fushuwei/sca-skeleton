package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户套餐实体。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_package")
public class SysTenantPackage extends BaseEntity {

    private String name;
    private String code;
    /** 套餐状态（enabled 启用，disabled 禁用） */
    private String status;
    /** 用户数限制，-1 表示不限 */
    private Integer userLimit;
    /** API 调用限制/日，-1 表示不限 */
    private Integer apiLimit;
    /** 存储限制(GB)，-1 表示不限 */
    private Integer storageLimit;
    /** 有效期天数，-1 表示不限 */
    private Integer expireDays;
    private Integer sort;
    private String remark;

    /** 关联的权限数量（非表字段，仅用于排序和展示） */
    @TableField(value = "permission_count",
                insertStrategy = FieldStrategy.NEVER,
                updateStrategy = FieldStrategy.NEVER,
                select = false)
    private Integer permissionCount;

    @Version
    private Integer version;
}
