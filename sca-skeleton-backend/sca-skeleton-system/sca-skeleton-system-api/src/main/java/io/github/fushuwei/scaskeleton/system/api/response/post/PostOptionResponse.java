package io.github.fushuwei.scaskeleton.system.api.response.post;

import lombok.Data;

/**
 * 岗位选项响应对象
 *
 * @author Fu Wei
 */
@Data
public class PostOptionResponse {

    /** 岗位 ID */
    private String id;

    /** 岗位名称 */
    private String name;

    /** 岗位编码 */
    private String code;

    /** 排序号 */
    private Integer sort;
}
