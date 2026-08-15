package io.github.fushuwei.scaskeleton.system.api.response.user;

import lombok.Builder;
import lombok.Data;

/**
 * 当前登录用户基本信息响应对象
 *
 * @author Fu Wei
 */
@Data
@Builder
public class UserProfileResponse {

    /** 用户 ID */
    private String id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 用户类型 */
    private String userType;

    /** 租户 ID */
    private String tenantId;

    /** 租户名称 */
    private String tenantName;
}
