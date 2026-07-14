package io.github.fushuwei.scaskeleton.system.api.remote;

import io.github.fushuwei.scaskeleton.system.api.response.permission.PermissionResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 权限管理远程调用服务
 *
 * @author Fu Wei
 */
@HttpExchange("/permission")
public interface SysPermissionRemoteService {

    /**
     * 查询全部权限列表
     *
     * @return 权限列表
     */
    @GetExchange("/list")
    List<PermissionResponse> list();

    /**
     * 通过 ID 查询权限信息
     *
     * @param id 权限 ID
     * @return 权限信息
     */
    @GetExchange("/{id}")
    PermissionResponse getById(@PathVariable("id") String id);
}
