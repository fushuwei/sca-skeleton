package io.github.fushuwei.scaskeleton.system.api.response.tenant;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户管理响应对象。
 *
 * @author Fu Wei
 */
@Data
public class TenantResponse {

    private String id;
    private String name;
    private String code;
    private String packageId;
    /** 套餐名称（关联查询，非表字段） */
    private String packageName;
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
    private String remark;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
}
