package io.github.fushuwei.scaskeleton.system.api.response.dept;

import lombok.Data;

/**
 * 部门选项响应对象
 *
 * @author Fu Wei
 */
@Data
public class DeptOptionResponse {

    /** 部门 ID */
    private String id;

    /** 上级部门 ID（顶级为 "0"） */
    private String parentId;

    /** 部门名称 */
    private String name;

    /** 排序号 */
    private Integer sort;

    /** 该部门（含子部门）用户数 */
    private Integer userCount;
}
