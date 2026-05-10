package io.github.fushuwei.sca.system.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.sca.starter.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位实体。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_post")
public class SysPost extends BaseEntity {

    private String tenantId;
    private String name;
    private String code;
    private Integer sort;
    private String remark;

    @Version
    private Integer version;
}
