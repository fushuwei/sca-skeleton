package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户套餐权限关联实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_package_permission")
public class SysTenantPackagePermission extends BaseEntity {

    /** 套餐 ID */
    private String packageId;

    /** 权限 ID */
    private String permissionId;
}
