package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.role.RolePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RoleCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RolePermissionAssignRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RoleUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.role.RoleOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.role.RoleResponse;
import io.github.fushuwei.scaskeleton.system.converter.RoleConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysRole;
import io.github.fushuwei.scaskeleton.system.entity.SysRolePermission;
import io.github.fushuwei.scaskeleton.system.entity.SysTenant;
import io.github.fushuwei.scaskeleton.system.entity.SysUserRole;
import io.github.fushuwei.scaskeleton.system.mapper.SysRoleMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysRolePermissionMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysTenantMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserRoleMapper;
import io.github.fushuwei.scaskeleton.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 角色管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;

    private final SysRolePermissionMapper rolePermissionMapper;

    private final SysTenantMapper tenantMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final RoleConverter roleConverter;

    /**
     * 查询角色列表
     *
     * @return 角色列表
     */
    @Override
    public List<RoleResponse> listRoles() {
        // 数据隔离：超管看所有租户，非超管只看自己租户
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
            .eq(!SecurityUtils.isSuperAdmin(), SysRole::getTenantId, SecurityUtils.getTenantId())
            .orderByAsc(SysRole::getSort));
        // 转换为响应对象列表
        return roles.stream().map(roleConverter::toRoleResponse).toList();
    }

    /**
     * 查询角色选项列表
     *
     * @return 角色选项列表
     */
    @Override
    public List<RoleOptionResponse> listRoleOptions() {
        List<SysRole> roles;

        if (SecurityUtils.isSuperAdmin()) {
            // 超级管理员：返回所有角色
            roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSort));
        } else {
            // 非超级管理员：仅返回当前用户自身拥有的角色（防止越权授予自己不具备的角色）
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
            // 按当前用户拥有的角色 ID 查询，并按租户隔离过滤（防御性：user_role 应该只含本租户角色，但保持一致性）
            String tenantId = SecurityUtils.getTenantId();
            roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getTenantId, tenantId)
                .orderByAsc(SysRole::getSort));
        }

        // 转换为响应对象列表
        return roleConverter.toRoleOptionResponseList(roles);
    }

    /**
     * 分页查询角色列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<RoleResponse> pageRoles(RolePageRequest request) {
        // 构造分页对象
        Page<RoleResponse> page = new Page<>(request.getPageNum(), request.getPageSize());
        // 数据隔离：超管看所有租户，非超管只看自己租户
        String tenantId = SecurityUtils.isSuperAdmin() ? null : SecurityUtils.getTenantId();
        // 查询分页数据
        return roleMapper.selectRolePage(page, tenantId, request);
    }

    /**
     * 根据 ID 查询角色详情
     *
     * @param id 角色 ID
     * @return 角色详情
     */
    @Override
    public RoleResponse getRoleById(String id) {
        // 加载角色实体并转换为响应对象
        return roleConverter.toRoleResponse(loadRoleEntity(id));
    }

    /**
     * 新增角色
     *
     * @param request 角色信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleCreateRequest request) {
        // 获取租户 ID（如果是超管创建，该值由前端页面传入，如果是租户内部用户自己创建，则取当前登录人所在租户的 ID）
        String tenantId = resolveTenantId(request.getTenantId());

        // 角色编码在同一个租户内唯一
        long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getTenantId, tenantId)
            .eq(SysRole::getCode, request.getCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "角色编码已存在");
        }

        // 封装角色实体
        SysRole role = new SysRole();
        role.setTenantId(tenantId);
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDataScope(request.getDataScope());
        role.setSort(request.getSort() != null ? request.getSort() : 100);
        role.setRemark(request.getRemark());
        role.setIsBuiltin(0);

        // 保存角色
        roleMapper.insert(role);

        // 保存关联关系
        saveRolePermissions(tenantId, role.getId(), request.getPermissionIds());
    }

    /**
     * 编辑角色
     *
     * @param request 角色信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleUpdateRequest request) {
        // 加载可操作角色实体
        SysRole role = loadOperableRoleEntity(request.getId());

        // 更新字段
        role.setName(request.getName());
        role.setDataScope(request.getDataScope());
        role.setSort(request.getSort() != null ? request.getSort() : role.getSort());
        role.setRemark(request.getRemark());

        // 更新角色
        roleMapper.updateById(role);

        // 删除旧的关联关系，并保存新的关联关系
        deleteRolePermissions(request.getId());
        saveRolePermissions(role.getTenantId(), request.getId(), request.getPermissionIds());
    }

    /**
     * 删除角色
     *
     * @param id 角色 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(String id) {
        // 加载可操作角色实体
        SysRole role = loadOperableRoleEntity(id);

        // 删除角色
        roleMapper.deleteById(role.getId());

        // 删除关联关系
        deleteRolePermissions(role.getId());
    }

    /**
     * 批量删除角色
     *
     * @param ids 角色 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteRoles(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        for (String id : ids) {
            deleteRole(id);
        }
    }

    /**
     * 查询角色已分配的权限 ID 列表
     *
     * @param roleId 角色 ID
     * @return 权限 ID 列表
     */
    @Override
    public List<String> getRolePermissionIds(String roleId) {
        // 校验角色存在
        loadRoleEntity(roleId);

        // 查询角色已分配的权限 ID 列表
        List<SysRolePermission> list = rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
            .eq(SysRolePermission::getRoleId, roleId));
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(SysRolePermission::getPermissionId).toList();
    }

    /**
     * 为角色分配权限
     *
     * @param request 权限分配信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(RolePermissionAssignRequest request) {
        // 加载可操作角色实体
        SysRole role = loadOperableRoleEntity(request.getId());

        // 先清空该角色下原有权限关联关系
        deleteRolePermissions(request.getId());

        // 保存新的角色与权限关联关系
        saveRolePermissions(role.getTenantId(), request.getId(), request.getPermissionIds());
    }

    /**
     * 保存角色与权限的关联关系
     *
     * @param tenantId      租户 ID
     * @param roleId        角色 ID
     * @param permissionIds 权限 ID 列表
     */
    private void saveRolePermissions(String tenantId, String roleId, List<String> permissionIds) {
        // 保存角色与权限关联关系
        if (!CollectionUtils.isEmpty(permissionIds)) {
            permissionIds.forEach(permId -> {
                SysRolePermission rp = new SysRolePermission();
                rp.setTenantId(tenantId);
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            });
        }
    }

    /**
     * 删除角色与权限的关联关系
     *
     * @param roleId 角色 ID
     */
    private void deleteRolePermissions(String roleId) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
            .eq(SysRolePermission::getRoleId, roleId));
    }

    /**
     * 解析创建时的目标租户 ID
     *
     * @param requestTenantId 创建时传入的目标租户 ID（仅当超管创建时才会使用该参数）
     * @return 实际写入用的租户 ID
     */
    private String resolveTenantId(String requestTenantId) {
        if (SecurityUtils.isSuperAdmin()) {
            if (!StringUtils.hasText(requestTenantId)) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "超管创建需指定目标租户");
            }
            SysTenant tenant = tenantMapper.selectById(requestTenantId);
            if (tenant == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "目标租户不存在");
            }
            return requestTenantId;
        }
        return SecurityUtils.getTenantId();
    }

    /**
     * 根据 ID 加载角色实体
     *
     * @param id 角色 ID
     * @return 角色实体
     */
    private SysRole loadRoleEntity(String id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        if (!SecurityUtils.isSuperAdmin()
            && !Objects.equals(role.getTenantId(), SecurityUtils.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
        }
        return role;
    }

    /**
     * 根据 ID 加载可操作角色实体
     *
     * @param id 角色 ID
     * @return 角色实体
     */
    private SysRole loadOperableRoleEntity(String id) {
        SysRole role = loadRoleEntity(id);
        if (role.getIsBuiltin() != null && role.getIsBuiltin() == 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "系统内置角色不允许操作");
        }
        return role;
    }
}
