package io.github.fushuwei.scaskeleton.system.api.response.dict;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典数据响应对象
 *
 * @author Fu Wei
 */
@Data
public class DictDataResponse {

    /** 字典数据 ID */
    private String id;

    /** 租户 ID */
    private String tenantId;

    /** 字典 ID */
    private String dictId;

    /** 字典标签 */
    private String label;

    /** 字典值 */
    private String value;

    /** 状态（enabled 启用，disabled 禁用） */
    private String status;

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 创建人 ID */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新人 ID */
    private String updateBy;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
