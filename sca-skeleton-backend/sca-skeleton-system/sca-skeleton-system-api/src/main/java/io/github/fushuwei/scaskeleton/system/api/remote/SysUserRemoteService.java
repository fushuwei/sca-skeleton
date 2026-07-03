package io.github.fushuwei.scaskeleton.system.api.remote;

import io.github.fushuwei.scaskeleton.system.api.response.user.UserProfileResponse;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * System 服务的用户管理远程调用契约。
 * <p>
 * 该接口由 {@code sca-skeleton-system-api} 模块提供，其他微服务通过引入此模块依赖，
 * 配合 {@code RestClient} + {@code HttpServiceProxyFactory} 注入即可像调用本地方法一样调用 system 服务。
 * <p>
 * 响应体 {@code Result<T>} 由 {@code RemoteResponseInterceptor} 自动解包，调用方直接获得 {@code T}。
 *
 * @author Fu Wei
 */
@HttpExchange(url = "/user")
public interface SysUserRemoteService {

    /**
     * 获取当前登录用户资料。
     *
     * @return 用户资料
     */
    @GetExchange("/profile")
    UserProfileResponse getProfile();

    /**
     * 按 ID 查询用户详情。
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @GetExchange("/{id}")
    UserResponse getById(@PathVariable("id") String id);
}
