package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.permission.PermissionPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.permission.PermissionCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.permission.PermissionUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.permission.PermissionResponse;

import java.util.List;

/**
 * 权限管理 Service
 *
 * @author Fu Wei
 */
public interface SysPermissionService {

    /**
     * 查询全量权限列表
     *
     * @return 权限列表
     */
    List<PermissionResponse> listAllPermissions();

    /**
     * 查询当前用户菜单列表
     *
     * @return 菜单列表
     */
    List<PermissionResponse> listUserMenus();

    /**
     * 分页查询权限列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<PermissionResponse> pagePermissions(PermissionPageRequest request);

    /**
     * 查询指定父节点下的按钮权限列表
     *
     * @param parentId 父权限 ID
     * @return 按钮权限列表
     */
    List<PermissionResponse> listButtonsByParentId(String parentId);

    /**
     * 根据 ID 查询权限详情
     *
     * @param id 权限 ID
     * @return 权限详情
     */
    PermissionResponse getPermissionById(String id);

    /**
     * 新增权限
     *
     * @param request 权限信息
     */
    void createPermission(PermissionCreateRequest request);

    /**
     * 编辑权限
     *
     * @param request 权限信息
     */
    void updatePermission(PermissionUpdateRequest request);

    /**
     * 删除权限
     *
     * @param id 权限 ID
     */
    void deletePermission(String id);

    /**
     * 批量删除权限
     *
     * @param ids 权限 ID 列表
     */
    void batchDeletePermissions(List<String> ids);
}
