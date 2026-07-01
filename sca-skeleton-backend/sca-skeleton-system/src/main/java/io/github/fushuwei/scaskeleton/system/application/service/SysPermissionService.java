package io.github.fushuwei.scaskeleton.system.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.dto.permission.PermissionPageRequest;
import io.github.fushuwei.scaskeleton.system.api.dto.permission.PermissionSaveRequest;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysPermission;

import java.util.List;

/**
 * 权限管理服务接口。
 *
 * @author Fu Wei
 */
public interface SysPermissionService {

    /** 查询全量权限树（用于权限分配界面） */
    List<SysPermission> listAllPermissions();

    /** 分页查询指定父节点下的子权限列表 */
    IPage<SysPermission> pagePermissions(PermissionPageRequest request);

    /** 查询指定父节点下的按钮权限列表（用于列表行展开） */
    List<SysPermission> listButtonsByParentId(String parentId);

    SysPermission getPermissionById(String id);

    void createPermission(PermissionSaveRequest request);

    void updatePermission(PermissionSaveRequest request);

    void deletePermission(String id);
}
