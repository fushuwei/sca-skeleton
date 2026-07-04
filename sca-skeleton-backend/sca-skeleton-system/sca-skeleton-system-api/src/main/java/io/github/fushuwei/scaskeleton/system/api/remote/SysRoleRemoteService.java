package io.github.fushuwei.scaskeleton.system.api.remote;

import io.github.fushuwei.scaskeleton.system.api.response.role.RoleResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * System 服务的角色管理远程调用契约。
 * <p>
 * 其他微服务通过引入 {@code sca-skeleton-system-api} 依赖，配合 RestClient 注入即可调用。
 * 响应体 {@code Result<T>} 由 {@code RemoteResponseInterceptor} 自动解包。
 *
 * @author Fu Wei
 */
@HttpExchange("/role")
public interface SysRoleRemoteService {

    /**
     * 按 ID 查询角色详情。
     *
     * @param id 角色 ID
     * @return 角色详情
     */
    @GetExchange("/{id}")
    RoleResponse getById(@PathVariable("id") String id);

    /**
     * 查询当前租户下角色列表。
     *
     * @return 角色列表
     */
    @GetExchange("/list")
    List<RoleResponse> list();
}
