package io.github.fushuwei.scaskeleton.core.result;

/**
 * 响应类型枚举
 *
 * @author Fu Wei
 */
public enum ResultType {

    /**
     * 成功：操作正常完成
     */
    SUCCESS,

    /**
     * 警告：操作完成但有需要关注的信息
     */
    WARNING,

    /**
     * 二次确认：需要用户确认后重试
     */
    CONFIRM,

    /**
     * 失败：操作未完成
     */
    FAILURE,
}
