package io.github.fushuwei.scaskeleton.system.api.response.notice;

import lombok.Data;

/**
 * 通知用户选项响应对象
 * <p>
 * 用于通知公告「接收范围=指定用户」时的用户搜索下拉：
 * 前端展示 username/nickname，选中后保存 {@code id}（用户 UUID）到接收目标。
 *
 * @author Fu Wei
 */
@Data
public class UserOptionResponse {

    /** 用户 ID（UUID，实际保存到接收目标的值） */
    private String id;

    /** 登录用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 真实姓名 */
    private String realName;
}
