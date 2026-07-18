package io.github.fushuwei.scaskeleton.system.api.response.post;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位响应对象
 *
 * @author Fu Wei
 */
@Data
public class PostResponse {

    /** 岗位 ID */
    private String id;

    /** 租户 ID */
    private String tenantId;

    /** 租户名称 */
    private String tenantName;

    /** 岗位名称 */
    private String name;

    /** 岗位编码 */
    private String code;

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;

    /** 创建人 ID */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新人 ID */
    private String updateBy;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
