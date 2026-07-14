package io.github.fushuwei.scaskeleton.system.api.remote;

import io.github.fushuwei.scaskeleton.system.api.response.role.RoleResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 角色管理远程调用服务
 *
 * @author Fu Wei
 */
@HttpExchange("/role")
public interface SysRoleRemoteService {

    /**
     * 查询当前租户下角色列表
     *
     * @return 角色列表
     */
    @GetExchange("/list")
    List<RoleResponse> list();

    /**
     * 通过 ID 查询角色信息
     *
     * @param id 角色 ID
     * @return 角色信息
     */
    @GetExchange("/{id}")
    RoleResponse getById(@PathVariable String id);
}
