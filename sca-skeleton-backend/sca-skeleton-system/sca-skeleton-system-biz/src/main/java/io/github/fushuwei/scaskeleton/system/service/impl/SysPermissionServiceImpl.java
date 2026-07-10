package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.permission.PermissionPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.permission.PermissionCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.permission.PermissionUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.permission.PermissionResponse;
import io.github.fushuwei.scaskeleton.system.converter.PermissionConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysPermission;
import io.github.fushuwei.scaskeleton.system.entity.SysRolePermission;
import io.github.fushuwei.scaskeleton.system.entity.SysUserRole;
import io.github.fushuwei.scaskeleton.system.mapper.SysPermissionMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysRolePermissionMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserRoleMapper;
import io.github.fushuwei.scaskeleton.system.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限管理服务实现。
 *
 * @author Fu Wei
 */
@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl implements SysPermissionService {

    /** 权限主表 Mapper */
    private final SysPermissionMapper permissionMapper;
    /** Entity ↔ Response 转换器（MapStruct 生成） */
    private final PermissionConverter permissionConverter;
    /** 用户角色关联 Mapper */
    private final SysUserRoleMapper userRoleMapper;
    /** 角色权限关联 Mapper */
    private final SysRolePermissionMapper rolePermissionMapper;

    @Override
    public List<PermissionResponse> listAllPermissions() {
        // 查询全局权限树（不按租户隔离），按 sort 升序
        List<SysPermission> permissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getSort));
        // 转换为响应对象列表
        return permissions.stream().map(permissionConverter::toPermissionResponse).toList();
    }

    @Override
    public List<PermissionResponse> listUserMenus() {
        // 超级管理员直接返回所有权限
        if (SecurityUtils.isSuperAdmin()) {
            List<SysPermission> permissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getStatus, "enabled")
                    .orderByAsc(SysPermission::getSort));
            return permissions.stream().map(permissionConverter::toPermissionResponse).toList();
        }

        // 普通用户根据角色获取权限
        String userId = SecurityUtils.getUserId();
        if (!StringUtils.hasText(userId)) {
            return Collections.emptyList();
        }

        // 查询用户的角色列表
        List<SysUserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        if (CollectionUtils.isEmpty(userRoles)) {
            return Collections.emptyList();
        }

        // 获取角色ID列表
        List<String> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .toList();

        // 查询角色关联的权限ID列表
        List<SysRolePermission> rolePermissions = rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                .in(SysRolePermission::getRoleId, roleIds));
        if (CollectionUtils.isEmpty(rolePermissions)) {
            return Collections.emptyList();
        }

        // 去重权限ID
        List<String> permissionIds = rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .distinct()
                .toList();

        // 查询权限详情（所有类型，前端负责过滤）
        List<SysPermission> permissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .in(SysPermission::getId, permissionIds)
                .eq(SysPermission::getStatus, "enabled")
                .orderByAsc(SysPermission::getSort));

        // 转换为响应对象列表
        return permissions.stream().map(permissionConverter::toPermissionResponse).toList();
    }

    @Override
    public IPage<PermissionResponse> pagePermissions(PermissionPageRequest req) {
        // 构造分页对象
        Page<SysPermission> page = new Page<>(req.getPageNum(), req.getPageSize());

        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
                // 按父节点筛选子权限，parentId 为空时不按父节点过滤（返回全部记录）
                .eq(StringUtils.hasText(req.getParentId()), SysPermission::getParentId, req.getParentId())
                // 关键词模糊匹配名称（中文/英文）或权限标识
                .and(StringUtils.hasText(req.getKeyword()),
                        w -> w.like(SysPermission::getName, req.getKeyword())
                                .or().like(SysPermission::getNameEn, req.getKeyword())
                                .or().like(SysPermission::getCode, req.getKeyword()))
                // 类型筛选
                .eq(StringUtils.hasText(req.getType()), SysPermission::getType, req.getType())
                // 状态筛选
                .eq(StringUtils.hasText(req.getStatus()), SysPermission::getStatus, req.getStatus());

        // 安全排序：白名单校验通过后按指定字段排序，否则按 sort 升序
        String orderBy = req.safeOrderBy();
        boolean isAsc = "ASC".equalsIgnoreCase(req.safeOrderDirection());
        if (orderBy != null) {
            switch (orderBy) {
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

        // 查询实体分页并转换为响应对象分页
        IPage<SysPermission> entityPage = permissionMapper.selectPage(page, wrapper);
        return entityPage.convert(permissionConverter::toPermissionResponse);
    }

    @Override
    public List<PermissionResponse> listButtonsByParentId(String parentId) {
        // 查询指定父节点下的按钮权限，按 sort 升序
        List<SysPermission> buttons = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getParentId, parentId)
                .eq(SysPermission::getType, "button")
                .orderByAsc(SysPermission::getSort));
        // 转换为响应对象列表
        return buttons.stream().map(permissionConverter::toPermissionResponse).toList();
    }

    @Override
    public PermissionResponse getPermissionById(String id) {
        // 按主键查询权限并转换为响应对象
        return permissionConverter.toPermissionResponse(loadPermissionEntity(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPermission(PermissionCreateRequest req) {
        // 组装权限实体
        SysPermission permission = new SysPermission();
        permission.setParentId(req.getParentId());
        permission.setName(req.getName());
        permission.setNameEn(req.getNameEn());
        permission.setType(req.getType());
        permission.setCode(req.getCode());
        permission.setPath(req.getPath());
        permission.setComponent(req.getComponent());
        permission.setIcon(req.getIcon());
        permission.setSort(req.getSort() != null ? req.getSort() : 100);
        permission.setIsVisible(req.getIsVisible() != null ? req.getIsVisible() : 1);
        permission.setIsExternal(req.getIsExternal() != null ? req.getIsExternal() : 0);
        permission.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : "enabled");
        permission.setRemark(req.getRemark());

        // 设置临时 treePath（数据库字段 NOT NULL，需在插入前赋值，插入后立即更新为正确值）
        permission.setTreePath("");
        // 先插入以获取自增主键 ID
        permissionMapper.insert(permission);

        // 更新 treePath：父路径 + 当前 ID
        String treePath = buildTreePath(req.getParentId(), permission.getId());
        permission.setTreePath(treePath);
        permissionMapper.updateById(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(PermissionUpdateRequest req) {
        // 校验权限存在并加载当前快照（parentId / treePath 不在此接口变更）
        SysPermission existing = loadPermissionEntity(req.getId());
        existing.setName(req.getName());
        existing.setNameEn(req.getNameEn());
        existing.setCode(req.getCode());
        existing.setPath(req.getPath());
        existing.setComponent(req.getComponent());
        existing.setIcon(req.getIcon());
        existing.setSort(req.getSort() != null ? req.getSort() : existing.getSort());
        existing.setIsVisible(req.getIsVisible() != null ? req.getIsVisible() : existing.getIsVisible());
        existing.setIsExternal(req.getIsExternal() != null ? req.getIsExternal() : existing.getIsExternal());
        existing.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : existing.getStatus());
        existing.setRemark(req.getRemark());
        permissionMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(String id) {
        // 存在子权限时不允许删除
        long childCount = permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "请先删除子权限");
        }
        // 逻辑删除权限主表
        permissionMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeletePermissions(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        for (String id : ids) {
            deletePermission(id);
        }
    }

    /**
     * 根据父节点 ID 与当前节点 ID 拼接树路径。
     *
     * @param parentId  父权限 ID，根节点为 "0"
     * @param currentId 当前权限 ID
     * @return 逗号分隔的树路径，如 {@code 0,parentId,currentId}
     */
    private String buildTreePath(String parentId, String currentId) {
        // 根节点下直接挂载
        if ("0".equals(parentId)) {
            return "0," + currentId;
        }
        // 父节点存在则继承其 treePath
        SysPermission parent = permissionMapper.selectById(parentId);
        if (parent == null) {
            // 父节点缺失时降级为根路径
            return "0," + currentId;
        }
        return parent.getTreePath() + "," + currentId;
    }

    /**
     * 按主键加载权限实体（供内部业务逻辑使用，不对外暴露 Entity）。
     *
     * @param id 权限 ID
     * @return 权限实体
     * @throws BusinessException 权限不存在时抛出 NOT_FOUND
     */
    private SysPermission loadPermissionEntity(String id) {
        SysPermission perm = permissionMapper.selectById(id);
        if (perm == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "权限不存在");
        }
        return perm;
    }
}
