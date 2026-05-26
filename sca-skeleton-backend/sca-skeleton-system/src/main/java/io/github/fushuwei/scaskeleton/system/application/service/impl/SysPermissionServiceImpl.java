package io.github.fushuwei.scaskeleton.system.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.system.api.dto.permission.PermissionSaveRequest;
import io.github.fushuwei.scaskeleton.system.application.service.SysPermissionService;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysPermission;
import io.github.fushuwei.scaskeleton.system.infrastructure.mapper.SysPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

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

    @Override
    public List<SysPermission> listAllPermissions() {
        // 查询全局权限树（不按租户隔离），按 sort 升序
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getSort));
    }

    @Override
    public SysPermission getPermissionById(String id) {
        // 按主键查询权限
        SysPermission perm = permissionMapper.selectById(id);
        if (perm == null) {
            // 未命中则抛业务异常
            throw new BusinessException(ResultCode.NOT_FOUND, "权限不存在");
        }
        return perm;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPermission(PermissionSaveRequest req) {
        // 组装权限实体
        SysPermission permission = new SysPermission();
        permission.setParentId(req.getParentId());
        permission.setName(req.getName());
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

        // 先插入以获取自增主键 ID
        permissionMapper.insert(permission);

        // 更新 treePath：父路径 + 当前 ID
        String treePath = buildTreePath(req.getParentId(), permission.getId());
        permission.setTreePath(treePath);
        permissionMapper.updateById(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(PermissionSaveRequest req) {
        // 校验权限存在并加载当前快照（parentId / treePath 不在此接口变更）
        SysPermission existing = getPermissionById(req.getId());
        existing.setName(req.getName());
        existing.setCode(req.getCode());
        existing.setPath(req.getPath());
        existing.setComponent(req.getComponent());
        existing.setIcon(req.getIcon());
        existing.setSort(req.getSort() != null ? req.getSort() : existing.getSort());
        existing.setIsVisible(req.getIsVisible() != null ? req.getIsVisible() : existing.getIsVisible());
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
}
