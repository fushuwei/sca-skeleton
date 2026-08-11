package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.role.RolePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RoleCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RoleUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.role.RoleOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.role.RoleResponse;

import java.util.List;

/**
 * 角色管理 Service
 *
 * @author Fu Wei
 */
public interface SysRoleService {

    /**
     * 查询角色列表
     *
     * @return 角色列表
     */
    List<RoleResponse> listRoles();

    /**
     * 查询角色选项列表
     *
     * @param realm 角色域
     * @return 角色选项列表
     */
    List<RoleOptionResponse> listRoleOptions(String realm);

    /**
     * 分页查询角色列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<RoleResponse> pageRoles(RolePageRequest request);

    /**
     * 根据 ID 查询角色详情
     *
     * @param id 角色 ID
     * @return 角色详情
     */
    RoleResponse getRoleById(String id);

    /**
     * 新增角色
     *
     * @param request 角色信息
     */
    void createRole(RoleCreateRequest request);

    /**
     * 编辑角色
     *
     * @param request 角色信息
     */
    void updateRole(RoleUpdateRequest request);

    /**
     * 删除角色
     *
     * @param id 角色 ID
     */
    void deleteRole(String id);

    /**
     * 批量删除角色
     *
     * @param ids 角色 ID 列表
     */
    void batchDeleteRoles(List<String> ids);

    /**
     * 查询角色已分配的权限 ID 列表
     *
     * @param roleId 角色 ID
     * @return 权限 ID 列表
     */
    List<String> getRolePermissionIds(String roleId);

    /**
     * 查询角色自定义数据权限的部门 ID 列表
     *
     * @param roleId 角色 ID
     * @return 部门 ID 列表
     */
    List<String> getRoleDeptIds(String roleId);
}
