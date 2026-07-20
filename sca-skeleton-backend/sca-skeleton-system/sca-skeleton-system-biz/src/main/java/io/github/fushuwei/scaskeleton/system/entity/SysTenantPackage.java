package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.Reference;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.ReferencedBy;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户套餐实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_package")
@ReferencedBy({
    @Reference(table = "sys_tenant", column = "package_id", message = "套餐已被租户使用，无法删除")
})
public class SysTenantPackage extends BaseEntity {

    // ==================== 基本信息 ====================

    /** 套餐名称 */
    private String name;

    /** 套餐编码 */
    private String code;

    /** 状态 */
    private String status;

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;

    // ==================== 配额限制 ====================

    /** 用户数限制 */
    private Integer userLimit;

    /** 每日 API 调用限制 */
    private Integer apiLimit;

    /** 存储限制（GB） */
    private Integer storageLimit;

    /** 有效期天数 */
    private Integer expireDays;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
