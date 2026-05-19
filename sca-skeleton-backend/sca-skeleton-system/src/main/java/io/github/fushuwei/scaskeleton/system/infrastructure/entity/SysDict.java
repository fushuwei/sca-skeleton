package io.github.fushuwei.scaskeleton.system.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典实体。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict")
public class SysDict extends BaseEntity {

    private String tenantId;
    private String name;
    private String code;
    /** 状态：enabled / disabled（数据库存 TINYINT，0=disabled, 1=enabled；此处用 Integer 映射） */
    private Integer status;
    private Integer isBuiltin;
    private String remark;

    @Version
    private Integer version;
}
