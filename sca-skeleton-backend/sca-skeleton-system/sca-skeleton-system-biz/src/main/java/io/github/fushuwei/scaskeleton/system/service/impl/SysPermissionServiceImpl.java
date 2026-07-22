package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.permission.PermissionPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.permission.PermissionCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.permission.PermissionUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.permission.PermissionAssignOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.permission.PermissionResponse;
import io.github.fushuwei.scaskeleton.system.converter.PermissionConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysPermission;
import io.github.fushuwei.scaskeleton.system.entity.SysUserRole;
import io.github.fushuwei.scaskeleton.system.mapper.SysPermissionMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserRoleMapper;
import io.github.fushuwei.scaskeleton.mybatis.reference.ReferenceChecker;
import io.github.fushuwei.scaskeleton.system.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl implements SysPermissionService {

    private final SysPermissionMapper permissionMapper;

    private final PermissionConverter permissionConverter;

    private final SysUserRoleMapper userRoleMapper;

    private final ReferenceChecker referenceChecker;

    /**
     * 查询全量权限列表
     *
     * @param realm 权限域
     * @return 权限列表
     */
    @Override
    public List<PermissionResponse> listAllPermissions(String realm) {
        // 查询全局权限树（不按租户隔离）
        List<SysPermission> permissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
            .eq(StringUtils.hasText(realm), SysPermission::getRealm, realm)
            .orderByAsc(SysPermission::getSort));
        // 转换为响应对象列表
        return permissionConverter.toPermissionResponseList(permissions);
    }

    /**
     * 查询可授权权限列表（用于角色/套餐授权面板）
     *
     * @param realm 权限域
     * @return 可授权权限列表
     */
    @Override
    public List<PermissionAssignOptionResponse> listAssignablePermissions(String realm) {
        List<SysPermission> permissions;

        if (SecurityUtils.isSuperAdmin()) {
            // 超级管理员：返回所有「启用 + 可见」的权限
            permissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, "enabled")
                .eq(SysPermission::getIsVisible, 1)
                .eq(StringUtils.hasText(realm), SysPermission::getRealm, realm)
                .orderByAsc(SysPermission::getSort));
        } else {
            // 非超级管理员：仅返回当前用户自身拥有的权限（防止越权授予自己不具备的权限）
            String userId = SecurityUtils.getUserId();
            if (!StringUtils.hasText(userId)) {
                return Collections.emptyList();
            }
            List<SysUserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
            if (CollectionUtils.isEmpty(userRoles)) {
                return Collections.emptyList();
            }
            List<String> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
            permissions = permissionMapper.selectPermissionsByRoleIds(roleIds);
            // 按权限域过滤（非超管仅能授予与当前角色域一致的权限）
            if (StringUtils.hasText(realm)) {
                permissions = permissions.stream().filter(p -> realm.equals(p.getRealm())).toList();
            }
        }

        // 转换为响应对象列表
        return permissionConverter.toPermissionAssignOptionResponseList(permissions);
    }

    /**
     * 查询当前用户菜单列表
     *
     * @return 菜单列表
     */
    @Override
    public List<PermissionResponse> listUserMenus() {
        // 超级管理员直接返回所有权限
        if (SecurityUtils.isSuperAdmin()) {
            List<SysPermission> permissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, "enabled")
                .eq(SysPermission::getIsVisible, 1)
                .orderByAsc(SysPermission::getSort));
            return permissionConverter.toPermissionResponseList(permissions);
        }

        // 获取当前用户 ID
        String userId = SecurityUtils.getUserId();
        if (!StringUtils.hasText(userId)) {
            return Collections.emptyList();
        }

        // 查询用户的角色
        List<SysUserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getUserId, userId));
        if (CollectionUtils.isEmpty(userRoles)) {
            return Collections.emptyList();
        }

        // 通过角色查询权限
        List<String> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        List<SysPermission> permissions = permissionMapper.selectPermissionsByRoleIds(roleIds);

        // 转换为响应对象列表
        return permissionConverter.toPermissionResponseList(permissions);
    }

    /**
     * 分页查询权限列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<PermissionResponse> pagePermissions(PermissionPageRequest request) {
        // 构造分页对象
        Page<SysPermission> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
            // 按父节点筛选子权限，parentId 为空时不按父节点过滤（返回全部记录）
            .eq(StringUtils.hasText(request.getParentId()), SysPermission::getParentId, request.getParentId())
            // 关键词模糊匹配名称（中文/英文）或权限标识
            .and(StringUtils.hasText(request.getKeyword()),
                w -> w.like(SysPermission::getName, request.getKeyword())
                    .or().like(SysPermission::getNameEn, request.getKeyword())
                    .or().like(SysPermission::getCode, request.getKeyword()))
            // 类型筛选
            .eq(StringUtils.hasText(request.getType()), SysPermission::getType, request.getType())
            // 权限域筛选
            .eq(StringUtils.hasText(request.getRealm()), SysPermission::getRealm, request.getRealm())
            // 状态筛选
            .eq(StringUtils.hasText(request.getStatus()), SysPermission::getStatus, request.getStatus());

        // 安全排序：白名单校验通过后按指定字段排序，默认按 sort 升序
        String sortField = request.safeSortField();
        boolean isAsc = "ASC".equalsIgnoreCase(request.safeSortOrder());
        if (sortField != null) {
            switch (sortField) {
                case "name" -> wrapper.orderBy(true, isAsc, SysPermission::getName);
                case "name_en" -> wrapper.orderBy(true, isAsc, SysPermission::getNameEn);
                case "code" -> wrapper.orderBy(true, isAsc, SysPermission::getCode);
                case "type" -> wrapper.orderBy(true, isAsc, SysPermission::getType);
                case "sort" -> wrapper.orderBy(true, isAsc, SysPermission::getSort);
                case "status" -> wrapper.orderBy(true, isAsc, SysPermission::getStatus);
                case "create_time" -> wrapper.orderBy(true, isAsc, SysPermission::getCreateTime);
                case "tree_path" -> wrapper.orderBy(true, isAsc, SysPermission::getTreePath);
            }
        } else {
            wrapper.orderByAsc(SysPermission::getSort);
        }

        // 查询分页数据，并将结果转换为响应对象
        IPage<SysPermission> entityPage = permissionMapper.selectPage(page, wrapper);
        return entityPage.convert(permissionConverter::toPermissionResponse);
    }

    /**
     * 查询指定父节点下的按钮权限列表
     *
     * @param parentId 父权限 ID
     * @return 按钮权限列表
     */
    @Override
    public List<PermissionResponse> listButtonsByParentId(String parentId) {
        // 查询指定父节点下的按钮权限
        List<SysPermission> buttons = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
            .eq(SysPermission::getParentId, parentId)
            .eq(SysPermission::getType, "button")
            .orderByAsc(SysPermission::getSort));
        // 转换为响应对象列表
        return permissionConverter.toPermissionResponseList(buttons);
    }

    /**
     * 根据 ID 查询权限详情
     *
     * @param id 权限 ID
     * @return 权限详情
     */
    @Override
    public PermissionResponse getPermissionById(String id) {
        // 加载权限实体并转换为响应对象
        return permissionConverter.toPermissionResponse(loadPermissionEntity(id));
    }

    /**
     * 新增权限
     *
     * @param request 权限信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPermission(PermissionCreateRequest request) {
        // 封装权限实体
        SysPermission permission = new SysPermission();
        permission.setId(UuidUtils.nextSimpleStr());
        permission.setParentId(request.getParentId());
        permission.setName(request.getName());
        permission.setNameEn(request.getNameEn());
        permission.setType(request.getType());
        permission.setCode(request.getCode());
        permission.setRealm(request.getRealm());
        permission.setPath(request.getPath());
        permission.setComponent(request.getComponent());
        permission.setIcon(request.getIcon());
        permission.setSort(request.getSort() != null ? request.getSort() : 100);
        permission.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : 1);
        permission.setIsExternal(request.getIsExternal() != null ? request.getIsExternal() : 0);
        permission.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "enabled");
        permission.setRemark(request.getRemark());
        permission.setTreePath(buildTreePath(permission.getParentId(), permission.getId()));

        // 保存权限
        permissionMapper.insert(permission);
    }

    /**
     * 编辑权限
     *
     * @param request 权限信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(PermissionUpdateRequest request) {
        // 加载权限实体
        SysPermission permission = loadPermissionEntity(request.getId());

        // 保存旧 treePath（用于批量更新子孙节点）
        String oldTreePath = permission.getTreePath();

        // 处理上级权限变更
        boolean parentChanged = false;
        if (StringUtils.hasText(request.getParentId()) && !request.getParentId().equals(permission.getParentId())) {
            String newParentId = request.getParentId();

            // 上级权限不能是自己
            if (newParentId.equals(permission.getId())) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "上级权限不能选择自己");
            }

            // 上级权限不能是自己的下级权限
            if (!"0".equals(newParentId)) {
                SysPermission newParent = loadPermissionEntity(newParentId);
                if (newParent.getTreePath().startsWith(permission.getTreePath() + ",")) {
                    throw new BusinessException(ResultCode.VALIDATION_ERROR, "上级权限不能选择自己的下级权限");
                }
            }

            permission.setParentId(newParentId);
            parentChanged = true;
        }

        // 更新字段
        permission.setName(request.getName());
        permission.setNameEn(request.getNameEn());
        permission.setType(request.getType());
        permission.setCode(request.getCode());
        permission.setPath(request.getPath());
        permission.setComponent(request.getComponent());
        permission.setIcon(request.getIcon());
        permission.setSort(request.getSort() != null ? request.getSort() : permission.getSort());
        permission.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : permission.getIsVisible());
        permission.setIsExternal(request.getIsExternal() != null ? request.getIsExternal() : permission.getIsExternal());
        permission.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : permission.getStatus());
        permission.setRemark(request.getRemark());

        // 如果上级权限变更，重新生成 treePath
        if (parentChanged) {
            permission.setTreePath(buildTreePath(permission.getParentId(), permission.getId()));
        }

        // 乐观锁：使用前端回传的 version 作为 WHERE 条件，若版本不匹配则影响行数为 0，说明数据已被其他用户修改
        permission.setVersion(request.getVersion());
        int affectedRows = permissionMapper.updateById(permission);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }

        // 上级权限变更后，批量更新所有子孙节点的 tree_path 字段值
        if (parentChanged) {
            permissionMapper.updateDescendantsTreePath(oldTreePath, permission.getTreePath(), oldTreePath.length() + 1);
        }
    }

    /**
     * 删除权限
     *
     * @param id 权限 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(String id) {
        // 加载权限实体
        loadPermissionEntity(id);

        // 引用校验
        referenceChecker.check(SysPermission.class, id);

        // 删除权限
        permissionMapper.deleteById(id);
    }

    /**
     * 批量删除权限
     *
     * @param ids 权限 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeletePermissions(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // 批量加载权限实体并校验存在
        loadPermissionEntities(ids);

        // 引用校验
        referenceChecker.checkBatch(SysPermission.class, ids);

        // 批量删除权限
        permissionMapper.deleteBatchIds(ids);
    }

    /**
     * 生成当前节点的树路径
     *
     * @param parentId  父权限 ID，根节点为 "0"
     * @param currentId 当前权限 ID
     * @return 逗号分隔的树路径
     */
    private String buildTreePath(String parentId, String currentId) {
        // 根节点下直接挂载
        if ("0".equals(parentId)) {
            return "0," + currentId;
        }
        // 父节点存在则继承其 treePath
        SysPermission parent = permissionMapper.selectById(parentId);
        if (parent == null) {
            return "0," + currentId;
        }
        return parent.getTreePath() + "," + currentId;
    }

    /**
     * 根据 ID 加载权限实体
     *
     * @param id 权限 ID
     * @return 权限实体
     */
    private SysPermission loadPermissionEntity(String id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "权限不存在");
        }
        return permission;
    }

    /**
     * 根据 ID 列表批量加载权限实体并校验存在性
     *
     * @param ids 权限 ID 列表
     * @return 权限实体列表
     */
    private List<SysPermission> loadPermissionEntities(List<String> ids) {
        List<String> distinctIds = ids.stream().distinct().toList();
        List<SysPermission> entities = permissionMapper.selectBatchIds(distinctIds);
        if (entities.size() != distinctIds.size()) {
            Set<String> foundIds = entities.stream().map(SysPermission::getId).collect(Collectors.toSet());
            List<String> missing = distinctIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BusinessException(ResultCode.NOT_FOUND, "权限不存在，ID: " + String.join(", ", missing));
        }
        return entities;
    }
}
