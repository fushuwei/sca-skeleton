package io.github.fushuwei.scaskeleton.system.api.response.tenantpackage;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户套餐管理响应对象。
 *
 * @author Fu Wei
 */
@Data
public class TenantPackageResponse {

    private String id;
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
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 关联的权限数量 */
    private Integer permissionCount;
    private String createBy;
    private String updateBy;
}
