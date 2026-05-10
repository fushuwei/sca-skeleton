package io.github.fushuwei.sca.system.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.sca.starter.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门实体。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    private String tenantId;
    /** 上级部门ID，顶级为 "0" */
    private String parentId;
    private String name;
    private String code;
    private Integer sort;
    private String leader;
    private String phone;
    private String email;
    /** 状态：enabled / disabled */
    private String status;
    /** ID 层级路径，逗号分隔，如 0,100,1001 */
    private String treePath;

    @Version
    private Integer version;
}
