package io.github.fushuwei.scaskeleton.system.api.remote;

import io.github.fushuwei.scaskeleton.system.api.response.permission.PermissionResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * System 服务的权限管理远程调用契约。
 * <p>
 * 其他微服务通过引入 {@code sca-skeleton-system-api} 依赖，配合 RestClient 注入即可调用。
 * 响应体 {@code Result<T>} 由 {@code RemoteResponseInterceptor} 自动解包。
 *
 * @author Fu Wei
 */
@HttpExchange("/permission")
public interface SysPermissionRemoteService {

    /**
     * 查询全部权限列表（平台级权限定义，无租户隔离）。
     *
     * @return 权限列表
     */
    @GetExchange("/list")
    List<PermissionResponse> list();

    /**
     * 按 ID 查询权限详情。
     *
     * @param id 权限 ID
     * @return 权限详情
     */
    @GetExchange("/{id}")
    PermissionResponse getById(@PathVariable("id") String id);
}
