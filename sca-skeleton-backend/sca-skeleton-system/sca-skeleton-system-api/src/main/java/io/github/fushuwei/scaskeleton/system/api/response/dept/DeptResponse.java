package io.github.fushuwei.scaskeleton.system.api.response.dept;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门管理响应对象。
 *
 * @author Fu Wei
 */
@Data
public class DeptResponse {

    private String id;
    private String tenantId;
    /** 上级部门 ID，顶级为 "0" */
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
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
}
