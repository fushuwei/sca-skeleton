package io.github.fushuwei.scaskeleton.system.api.response.role;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色管理响应对象。
 *
 * @author Fu Wei
 */
@Data
public class RoleResponse {

    private String id;
    private String tenantId;
    private String name;
    private String code;
    /**
     * 数据权限范围：
     * all-全部，tenant-租户，dept_and_sub-本部门及下级，dept-仅本部门，
     * personal-仅本人，custom-自定义
     */
    private String dataScope;
    /** 是否系统内置：0-否，1-是，内置角色不允许删除 */
    private Integer isBuiltin;
    private Integer sort;
    private String remark;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
}
