package io.github.fushuwei.scaskeleton.mybatis.reference;

import lombok.Data;

/**
 * 批量删除时的引用检查结果
 *
 * @author Fu Wei
 */
@Data
public class ReferenceCheckBatchResult {

    /** 被删除实体的 ID */
    private String id;

    /** 被删除实体的展示字段值 */
    private String displayName;

    /** 校验失败时返回给用户的提示信息 */
    private String message;

    /** 是否存在引用记录 */
    private Boolean hasReference;
}
