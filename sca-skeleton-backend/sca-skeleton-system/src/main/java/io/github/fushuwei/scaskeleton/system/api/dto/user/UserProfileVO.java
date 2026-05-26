package io.github.fushuwei.scaskeleton.system.api.dto.user;

import lombok.Builder;
import lombok.Data;

/**
 * 当前登录用户资料 VO（供 SPA 登录后拉取）。
 *
 * @author Fu Wei
 */
@Data
@Builder
public class UserProfileVO {

    /** 用户业务 ID（与 token sub 一致） */
    private String id;

    /** 登录用户名 */
    private String username;

    /** 昵称（展示用） */
    private String nickname;
}
