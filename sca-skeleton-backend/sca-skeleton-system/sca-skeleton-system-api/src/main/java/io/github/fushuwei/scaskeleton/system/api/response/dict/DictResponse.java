package io.github.fushuwei.scaskeleton.system.api.response.dict;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典响应对象
 *
 * @author Fu Wei
 */
@Data
public class DictResponse {

    /** 字典 ID */
    private String id;

    /** 租户 ID */
    private String tenantId;

    /** 字典名称 */
    private String name;

    /** 字典编码 */
    private String code;

    /** 状态（enabled 启用，disabled 禁用） */
    private String status;

    /** 是否内置（0 否，1 是） */
    private Integer isBuiltin;

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
