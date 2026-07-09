package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.system.api.request.role.RolePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RoleCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RoleUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.role.RoleResponse;
import io.github.fushuwei.scaskeleton.system.converter.RoleConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysRole;
import io.github.fushuwei.scaskeleton.system.entity.SysRolePermission;
import io.github.fushuwei.scaskeleton.system.mapper.SysRoleMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysRolePermissionMapper;
import io.github.fushuwei.scaskeleton.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务实现。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    /** 角色主表 Mapper */
    private final SysRoleMapper roleMapper;
    /** 角色-权限关联 Mapper */
    private final SysRolePermissionMapper rolePermissionMapper;
    /** Entity ↔ Response 转换器（MapStruct 生成） */
    private final RoleConverter roleConverter;

    @Override
    public IPage<RoleResponse> pageRoles(String tenantId, RolePageRequest req) {
        // 构造分页对象
        Page<SysRole> page = new Page<>(req.getPageNum(), req.getPageSize());

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                // 按租户隔离
                .eq(SysRole::getTenantId, tenantId)
                // 关键词模糊匹配名称或编码
                .and(StringUtils.hasText(req.getKeyword()),
                        w -> w.like(SysRole::getName, req.getKeyword())
                                .or().like(SysRole::getCode, req.getKeyword()))
                // 数据权限范围筛选
                .eq(StringUtils.hasText(req.getDataScope()), SysRole::getDataScope, req.getDataScope());

        // 安全排序：白名单校验通过后按指定字段排序，否则按 sort 升序
        String orderBy = req.safeOrderBy();
        boolean isAsc = "ASC".equalsIgnoreCase(req.safeOrderDirection());
        if (orderBy != null) {
            switch (orderBy) {
                case "name" -> wrapper.orderBy(true, isAsc, SysRole::getName);
                case "code" -> wrapper.orderBy(true, isAsc, SysRole::getCode);
                case "data_scope" -> wrapper.orderBy(true, isAsc, SysRole::getDataScope);
                case "sort" -> wrapper.orderBy(true, isAsc, SysRole::getSort);
                case "create_time" -> wrapper.orderBy(true, isAsc, SysRole::getCreateTime);
                case "permission_count" -> wrapper.orderBy(true, isAsc, SysRole::getPermissionCount);
            }
        } else {
            wrapper.orderByAsc(SysRole::getSort);
        }

        // 查询实体分页并转换为响应对象分页
        IPage<SysRole> entityPage = roleMapper.selectRolePage(page, wrapper);
        return entityPage.convert(roleConverter::toRoleResponse);
    }

    @Override
    public List<RoleResponse> listRoles(String tenantId) {
        // 按租户查询全部角色，按 sort 升序
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .orderByAsc(SysRole::getSort));
        // 转换为响应对象列表
        return roles.stream().map(roleConverter::toRoleResponse).toList();
    }

    @Override
    public RoleResponse getRoleById(String id) {
        // 按主键查询角色并转换为响应对象
        return roleConverter.toRoleResponse(loadRoleEntity(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(String tenantId, RoleCreateRequest req) {
        // 角色编码在同租户内唯一
        long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getCode, req.getCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "角色编码已存在");
        }
        // 组装角色实体
        SysRole role = new SysRole();
        role.setTenantId(tenantId);
        role.setName(req.getName());
        role.setCode(req.getCode());
        role.setDataScope(req.getDataScope());
        role.setSort(req.getSort() != null ? req.getSort() : 100);
        role.setRemark(req.getRemark());
        role.setIsBuiltin(0);
        // 持久化角色主表
        roleMapper.insert(role);
        // 同事务内建立角色-权限关联
        saveRolePermissions(tenantId, role.getId(), req.getPermissionIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(String tenantId, RoleUpdateRequest req) {
        // 校验角色存在并加载当前快照（编码不可改，故不更新 code）
        SysRole existing = loadRoleEntity(req.getId());
        existing.setName(req.getName());
        existing.setDataScope(req.getDataScope());
        existing.setSort(req.getSort() != null ? req.getSort() : existing.getSort());
        existing.setRemark(req.getRemark());
        roleMapper.updateById(existing);
        // 清除旧关联，重新建立
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, req.getId()));
        saveRolePermissions(tenantId, req.getId(), req.getPermissionIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(String id) {
        // 加载待删角色并校验内置保护
        SysRole role = loadRoleEntity(id);
        if (role.getIsBuiltin() != null && role.getIsBuiltin() == 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "系统内置角色不允许删除");
        }
        // 逻辑删除角色主表
        roleMapper.deleteById(id);
        // 同事务内清理角色-权限关联
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
    }

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

    @Override
    public List<String> getRolePermissionIds(String roleId) {
        // 校验角色存在
        loadRoleEntity(roleId);
        // 查询角色已分配的权限 ID 列表
        List<SysRolePermission> list = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId));
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(String tenantId, String roleId, List<String> permissionIds) {
        // 校验角色存在
        loadRoleEntity(roleId);
        // 先清空该角色下原有权限关联（全量替换策略）
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getTenantId, tenantId)
                .eq(SysRolePermission::getRoleId, roleId));
        // 非空则逐条插入新的角色-权限关联
        saveRolePermissions(tenantId, roleId, permissionIds);
    }

    /**
     * 批量插入角色-权限关联记录。
     *
     * @param tenantId      租户 ID
     * @param roleId        角色 ID
     * @param permissionIds 权限 ID 列表，为空则不操作
     */
    private void saveRolePermissions(String tenantId, String roleId, List<String> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }
        permissionIds.forEach(permId -> {
            SysRolePermission rp = new SysRolePermission();
            rp.setTenantId(tenantId);
            rp.setRoleId(roleId);
            rp.setPermissionId(permId);
            rolePermissionMapper.insert(rp);
        });
    }

    /**
     * 按主键加载角色实体（供内部业务逻辑使用，不对外暴露 Entity）。
     *
     * @param id 角色 ID
     * @return 角色实体
     * @throws BusinessException 角色不存在时抛出 NOT_FOUND
     */
    private SysRole loadRoleEntity(String id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }
}
