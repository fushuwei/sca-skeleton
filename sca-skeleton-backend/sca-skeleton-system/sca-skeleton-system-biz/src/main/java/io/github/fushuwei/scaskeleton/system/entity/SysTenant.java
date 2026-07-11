package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户实体。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SysTenant extends BaseEntity {

    private String name;
    private String code;
    private String packageId;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String domainName;
    /** 账号数量限制，-1 表示不限 */
    private Integer accountLimit;
    /** 过期时间（NULL表示永不过期） */
    private LocalDateTime expireTime;
    /** 租户状态（normal 正常，disabled 禁用，expired 过期，cancelled 注销） */
    private String status;
    /** 租户个性化配置（Logo、主题、策略等），JSON 字符串 */
    private String configJson;
    private String remark;

    /** 套餐名称（非表字段，仅用于展示，由自定义 SQL 关联查询填充） */
    @TableField(value = "package_name",
                insertStrategy = FieldStrategy.NEVER,
                updateStrategy = FieldStrategy.NEVER,
                select = false)
    private String packageName;

    @Version
    private Integer version;
}
