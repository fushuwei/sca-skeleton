package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict")
public class SysDict extends BaseEntity {

    /** 租户 ID */
    private String tenantId;

    /** 字典名称 */
    private String name;

    /** 字典编码 */
    private String code;

    /** 状态 */
    private Integer status;

    /** 是否系统内置 */
    private Integer isBuiltin;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
