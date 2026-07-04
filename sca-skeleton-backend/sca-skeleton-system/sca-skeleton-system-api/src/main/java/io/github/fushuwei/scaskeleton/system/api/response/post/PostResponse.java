package io.github.fushuwei.scaskeleton.system.api.response.post;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位管理响应对象。
 *
 * @author Fu Wei
 */
@Data
public class PostResponse {

    private String id;
    private String tenantId;
    private String name;
    private String code;
    private Integer sort;
    private String remark;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
}
