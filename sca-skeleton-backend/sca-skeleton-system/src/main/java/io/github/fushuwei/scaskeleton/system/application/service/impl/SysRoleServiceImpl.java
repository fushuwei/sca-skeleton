package io.github.fushuwei.scaskeleton.system.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.exception.ErrorCode;
import io.github.fushuwei.scaskeleton.system.api.dto.role.RoleSaveRequest;
import io.github.fushuwei.scaskeleton.system.application.service.SysRoleService;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysRole;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysRolePermission;
import io.github.fushuwei.scaskeleton.system.infrastructure.mapper.SysRoleMapper;
import io.github.fushuwei.scaskeleton.system.infrastructure.mapper.SysRolePermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 角色管理服务实现。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    @Override
    public List<SysRole> listRoles(String tenantId) {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .orderByAsc(SysRole::getSort));
    }

    @Override
    public SysRole getRoleById(String id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(String tenantId, RoleSaveRequest req) {
        long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getCode, req.getCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setTenantId(tenantId);
        role.setName(req.getName());
        role.setCode(req.getCode());
        role.setDataScope(req.getDataScope());
        role.setSort(req.getSort() != null ? req.getSort() : 100);
        role.setRemark(req.getRemark());
        role.setIsBuiltin(0);
        roleMapper.insert(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(String tenantId, RoleSaveRequest req) {
        SysRole existing = getRoleById(req.getId());
        existing.setName(req.getName());
        existing.setDataScope(req.getDataScope());
        existing.setSort(req.getSort() != null ? req.getSort() : existing.getSort());
        existing.setRemark(req.getRemark());
        roleMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(String id) {
        SysRole role = getRoleById(id);
        if (role.getIsBuiltin() != null && role.getIsBuiltin() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统内置角色不允许删除");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(String tenantId, String roleId, List<String> permissionIds) {
        getRoleById(roleId);
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getTenantId, tenantId)
                .eq(SysRolePermission::getRoleId, roleId));
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
}
