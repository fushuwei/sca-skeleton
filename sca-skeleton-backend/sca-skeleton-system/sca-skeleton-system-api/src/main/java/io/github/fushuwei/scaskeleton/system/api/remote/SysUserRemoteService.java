package io.github.fushuwei.scaskeleton.system.api.remote;

import io.github.fushuwei.scaskeleton.system.api.response.user.UserProfileResponse;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 用户管理远程调用服务
 *
 * @author Fu Wei
 */
@HttpExchange("/user")
public interface SysUserRemoteService {

    /**
     * 获取当前登录用户基本信息
     *
     * @return 用户基本信息
     */
    @GetExchange("/profile")
    UserProfileResponse getProfile();

    /**
     * 根据 ID 查询用户信息
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    @GetExchange("/{id}")
    UserResponse getById(@PathVariable String id);
}
