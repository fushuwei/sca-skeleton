package io.github.fushuwei.scaskeleton.mybatis.reference.dto;

import lombok.Data;

/**
 * 单条引用检查结果
 *
 * @author Fu Wei
 */
@Data
public class ReferenceCheckResult {

    /** 校验失败时返回给用户的提示信息 */
    private String message;

    /** 是否存在引用记录 */
    private Boolean hasReference;
}
