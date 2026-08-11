package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.role.RolePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RoleCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RoleUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.role.RoleOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.role.RoleResponse;
import io.github.fushuwei.scaskeleton.system.converter.RoleConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysRole;
import io.github.fushuwei.scaskeleton.system.entity.SysDept;
import io.github.fushuwei.scaskeleton.system.entity.SysPermission;
import io.github.fushuwei.scaskeleton.system.entity.SysRoleDataScope;
import io.github.fushuwei.scaskeleton.system.entity.SysRolePermission;
import io.github.fushuwei.scaskeleton.system.entity.SysUserRole;
import io.github.fushuwei.scaskeleton.system.mapper.SysRoleMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysDeptMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysPermissionMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysRoleDataScopeMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysRolePermissionMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserRoleMapper;
import io.github.fushuwei.scaskeleton.mybatis.reference.ReferenceChecker;
import io.github.fushuwei.scaskeleton.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final SysPermissionMapper permissionMapper;

    private final SysRolePermissionMapper rolePermissionMapper;

    private final SysRoleDataScopeMapper roleDataScopeMapper;

    private final SysDeptMapper deptMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final RoleConverter roleConverter;

    private final ReferenceChecker referenceChecker;

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
        return roleConverter.toRoleResponseList(roles);
    }

    /**
     * 查询角色选项列表
     *
     * @param tenantId 目标租户 ID（超管必传，未传返回空；非超管忽略，使用登录人所属的租户）
     * @param realm    角色域
     * @return 角色选项列表
     */
    @Override
    public List<RoleOptionResponse> listRoleOptions(String tenantId, String realm) {
        List<SysRole> roles;

        if (SecurityUtils.isSuperAdmin()) {
            // 超级管理员：必须指定目标租户，未传则返回空（防止超管在未选租户时看到所有租户数据导致越权分配）
            if (!StringUtils.hasText(tenantId)) {
                return Collections.emptyList();
            }
            roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(StringUtils.hasText(realm), SysRole::getRealm, realm)
                .orderByAsc(SysRole::getSort));
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
            // 按当前用户拥有的角色 ID 查询，并按租户隔离过滤（非超管忽略传入的 tenantId，使用自身租户）
            roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getTenantId, SecurityUtils.getTenantId())
                .eq(StringUtils.hasText(realm), SysRole::getRealm, realm)
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
        // 获取当前登录用户所在租户的 ID
        String tenantId = SecurityUtils.getTenantId();

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
        role.setRealm(request.getRealm());
        role.setSort(request.getSort() != null ? request.getSort() : 100);
        role.setRemark(request.getRemark());
        role.setIsBuiltin(0);

        // 校验分配的权限不超出租户套餐（超管）或用户自身权限（非超管）范围
        validateAssignablePermissions(tenantId, request.getPermissionIds(), request.getRealm());

        // 解析并校验数据权限部门：custom 必须至少选择一个部门，非 custom 忽略传入的部门列表
        List<String> deptIds = resolveDataScopeDeptIds(request.getDataScope(), request.getDeptIds());

        // 校验自定义数据权限所选部门均属于当前租户
        validateDeptScope(tenantId, deptIds);

        // 保存角色
        roleMapper.insert(role);

        // 保存关联关系
        saveRolePermissions(tenantId, role.getId(), request.getPermissionIds());

        // 保存自定义数据权限部门关联关系
        saveRoleDataScopes(tenantId, role.getId(), deptIds);
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

        // 先校验再写库（与 createRole 的"先校验后写库"顺序保持一致），校验失败时事务回滚，避免无意义的写库

        // 校验分配的权限不超出租户套餐（超管）或用户自身权限（非超管）范围
        validateAssignablePermissions(role.getTenantId(), request.getPermissionIds(), role.getRealm());

        // 解析并校验数据权限部门：custom 必须至少选择一个部门，非 custom 忽略传入的部门列表
        List<String> deptIds = resolveDataScopeDeptIds(request.getDataScope(), request.getDeptIds());

        // 校验自定义数据权限所选部门均属于角色所在租户
        validateDeptScope(role.getTenantId(), deptIds);

        // 乐观锁：使用前端回传的 version 作为 WHERE 条件，若版本不匹配则影响行数为 0，说明数据已被其他用户修改
        role.setVersion(request.getVersion());
        int affectedRows = roleMapper.updateById(role);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }

        // 删除旧的关联关系，并保存新的关联关系
        deleteRolePermissions(request.getId());
        saveRolePermissions(role.getTenantId(), request.getId(), request.getPermissionIds());

        // 删除旧的自定义数据权限部门关联，并保存新的关联关系
        deleteRoleDataScopes(request.getId());
        saveRoleDataScopes(role.getTenantId(), request.getId(), deptIds);
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

        // 引用校验
        referenceChecker.check(SysRole.class, id);

        // 删除关联关系
        deleteRolePermissions(role.getId());
        deleteRoleDataScopes(role.getId());

        // 删除角色
        roleMapper.deleteById(role.getId());
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

        // 批量加载角色实体并校验存在、租户隔离与内置角色
        loadOperableRoleEntities(ids);

        // 引用校验
        referenceChecker.checkBatch(SysRole.class, ids);

        // 批量删除关联关系
        deleteRolePermissions(ids);
        deleteRoleDataScopes(ids);

        // 批量删除角色
        roleMapper.deleteBatchIds(ids);
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
     * 查询角色自定义数据权限的部门 ID 列表
     *
     * @param roleId 角色 ID
     * @return 部门 ID 列表
     */
    @Override
    public List<String> getRoleDeptIds(String roleId) {
        // 校验角色存在
        loadRoleEntity(roleId);

        // 查询角色自定义数据权限的部门 ID 列表
        List<SysRoleDataScope> list = roleDataScopeMapper.selectList(new LambdaQueryWrapper<SysRoleDataScope>()
            .eq(SysRoleDataScope::getRoleId, roleId));
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(SysRoleDataScope::getDeptId).toList();
    }

    /**
     * 校验所选权限的域与角色域一致，防止跨域分配权限（越权防护）
     *
     * @param roleRealm     角色域
     * @param permissionIds 权限 ID 列表
     */
    private void validatePermissionRealm(String roleRealm, List<String> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }
        Set<String> permIdSet = new HashSet<>(permissionIds);
        long validCount = permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
            .in(SysPermission::getId, permIdSet)
            .eq(SysPermission::getRealm, roleRealm));
        if (validCount != permIdSet.size()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "所选权限与角色域不一致，不允许跨域分配权限");
        }
    }

    /**
     * 校验分配的权限不超出可授权范围（越权防护）
     * <p>
     * 超管：权限不能超过目标租户套餐所拥有的权限；<br>
     * 非超管：权限不能超过当前登录用户自身所拥有的权限。
     *
     * @param tenantId      目标租户 ID
     * @param permissionIds 权限 ID 列表
     * @param roleRealm     角色域（用于跨域校验）
     */
    private void validateAssignablePermissions(String tenantId, List<String> permissionIds, String roleRealm) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }
        // 1. 校验权限域与角色域一致，防止跨域分配权限
        validatePermissionRealm(roleRealm, permissionIds);
        // 2. 校验权限不超出可授权范围
        Set<String> allowedIds = resolveAllowedPermissionIds(tenantId);
        Set<String> requestedIds = new HashSet<>(permissionIds);
        if (!allowedIds.containsAll(requestedIds)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "存在超出可授权范围的权限，不允许分配");
        }
    }

    /**
     * 解析当前登录用户可授权的权限 ID 集合
     * <p>
     * 超管：目标租户套餐内的权限；<br>
     * 非超管：当前登录用户自身拥有的权限。
     *
     * @param tenantId 目标租户 ID
     * @return 可授权权限 ID 集合
     */
    private Set<String> resolveAllowedPermissionIds(String tenantId) {
        if (SecurityUtils.isSuperAdmin()) {
            return getTenantPackagePermissionIds(tenantId);
        }
        return getCurrentUserPermissionIds();
    }

    /**
     * 查询指定租户套餐内的权限 ID 集合（单条 JOIN SQL）
     *
     * @param tenantId 租户 ID
     * @return 权限 ID 集合
     */
    private Set<String> getTenantPackagePermissionIds(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return Collections.emptySet();
        }
        List<SysPermission> permissions = permissionMapper.selectPermissionsByTenantPackage(tenantId, null);
        return permissions.stream().map(SysPermission::getId).collect(Collectors.toSet());
    }

    /**
     * 查询当前登录用户自身拥有的权限 ID 集合
     *
     * @return 权限 ID 集合
     */
    private Set<String> getCurrentUserPermissionIds() {
        String userId = SecurityUtils.getUserId();
        if (!StringUtils.hasText(userId)) {
            return Collections.emptySet();
        }
        List<SysUserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getUserId, userId));
        if (CollectionUtils.isEmpty(userRoles)) {
            return Collections.emptySet();
        }
        List<String> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        List<SysPermission> permissions = permissionMapper.selectPermissionsByRoleIds(roleIds);
        return permissions.stream().map(SysPermission::getId).collect(Collectors.toSet());
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
        deleteRolePermissions(Collections.singletonList(roleId));
    }

    /**
     * 批量删除角色与权限的关联关系
     *
     * @param roleIds 角色 ID 列表
     */
    private void deleteRolePermissions(List<String> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
            .in(SysRolePermission::getRoleId, roleIds));
    }

    /**
     * 保存角色与自定义数据权限部门的关联关系
     *
     * @param tenantId 租户 ID
     * @param roleId   角色 ID
     * @param deptIds  部门 ID 列表
     */
    private void saveRoleDataScopes(String tenantId, String roleId, List<String> deptIds) {
        // 保存角色与部门关联关系（批量插入，避免循环逐条插入的性能开销；先去重，防止同一部门重复关联产生脏数据）
        if (!CollectionUtils.isEmpty(deptIds)) {
            List<SysRoleDataScope> scopes = deptIds.stream().distinct().map(deptId -> {
                SysRoleDataScope rds = new SysRoleDataScope();
                rds.setTenantId(tenantId);
                rds.setRoleId(roleId);
                rds.setDeptId(deptId);
                return rds;
            }).collect(Collectors.toList());
            roleDataScopeMapper.insertBatch(scopes);
        }
    }

    /**
     * 删除角色与自定义数据权限部门的关联关系
     *
     * @param roleId 角色 ID
     */
    private void deleteRoleDataScopes(String roleId) {
        deleteRoleDataScopes(Collections.singletonList(roleId));
    }

    /**
     * 批量删除角色与自定义数据权限部门的关联关系
     *
     * @param roleIds 角色 ID 列表
     */
    private void deleteRoleDataScopes(List<String> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        roleDataScopeMapper.delete(new LambdaQueryWrapper<SysRoleDataScope>()
            .in(SysRoleDataScope::getRoleId, roleIds));
    }

    /**
     * 解析并校验数据权限对应的部门 ID 列表，统一收敛数据权限与部门的业务约束：
     * <ul>
     *   <li>dataScope 为 custom 时，必须至少选择一个部门，否则抛出业务异常（防止落库"自定义但无部门"的脏数据）；</li>
     *   <li>dataScope 非 custom 时，忽略传入的部门 ID 列表并返回 null（不保存部门关联，与前端仅 custom 才提交 deptIds 的约定保持一致）。</li>
     * </ul>
     *
     * @param dataScope 数据权限范围
     * @param deptIds   请求中的部门 ID 列表
     * @return 有效的部门 ID 列表；非 custom 时返回 null
     */
    private List<String> resolveDataScopeDeptIds(String dataScope, List<String> deptIds) {
        if (!"custom".equals(dataScope)) {
            return null;
        }
        if (CollectionUtils.isEmpty(deptIds)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "自定义数据权限必须选择至少一个部门");
        }
        return deptIds;
    }

    /**
     * 校验自定义数据权限所选部门均属于指定租户，防止跨租户引用（越权防护）
     *
     * @param tenantId 租户 ID
     * @param deptIds  部门 ID 列表
     */
    private void validateDeptScope(String tenantId, List<String> deptIds) {
        if (CollectionUtils.isEmpty(deptIds)) {
            return;
        }
        Set<String> deptIdSet = new HashSet<>(deptIds);
        long validCount = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
            .in(SysDept::getId, deptIdSet)
            .eq(SysDept::getTenantId, tenantId));
        if (validCount != deptIdSet.size()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "所选部门不在当前租户范围内，不允许分配");
        }
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

    /**
     * 根据 ID 列表批量加载角色实体并校验存在性、租户隔离与内置角色
     *
     * @param ids 角色 ID 列表
     * @return 角色实体列表
     */
    private List<SysRole> loadOperableRoleEntities(List<String> ids) {
        List<String> distinctIds = ids.stream().distinct().toList();
        List<SysRole> entities = roleMapper.selectBatchIds(distinctIds);
        if (entities.size() != distinctIds.size()) {
            Set<String> foundIds = entities.stream().map(SysRole::getId).collect(Collectors.toSet());
            List<String> missing = distinctIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在，ID: " + String.join(", ", missing));
        }
        boolean isSuperAdmin = SecurityUtils.isSuperAdmin();
        String currentTenantId = isSuperAdmin ? null : SecurityUtils.getTenantId();
        for (SysRole entity : entities) {
            if (!isSuperAdmin && !Objects.equals(entity.getTenantId(), currentTenantId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
            }
            if (entity.getIsBuiltin() != null && entity.getIsBuiltin() == 1) {
                throw new BusinessException(ResultCode.FORBIDDEN, "系统内置角色不允许操作");
            }
        }
        return entities;
    }
}
