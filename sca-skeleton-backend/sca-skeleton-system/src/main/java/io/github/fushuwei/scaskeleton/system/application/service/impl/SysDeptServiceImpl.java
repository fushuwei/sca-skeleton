package io.github.fushuwei.scaskeleton.system.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.system.api.dto.dept.DeptSaveRequest;
import io.github.fushuwei.scaskeleton.system.application.service.SysDeptService;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysDept;
import io.github.fushuwei.scaskeleton.system.infrastructure.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 部门管理服务实现。
 *
 * @author Fu Wei
 */
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl implements SysDeptService {

    private final SysDeptMapper deptMapper;

    @Override
    public List<SysDept> listDepts(String tenantId) {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getTenantId, tenantId)
                .orderByAsc(SysDept::getSort));
    }

    @Override
    public SysDept getDeptById(String id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "部门不存在");
        }
        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDept(String tenantId, DeptSaveRequest req) {
        SysDept dept = new SysDept();
        dept.setTenantId(tenantId);
        dept.setParentId(req.getParentId());
        dept.setName(req.getName());
        dept.setCode(req.getCode());
        dept.setSort(req.getSort() != null ? req.getSort() : 100);
        dept.setLeader(req.getLeader());
        dept.setPhone(req.getPhone());
        dept.setEmail(req.getEmail());
        dept.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : "enabled");
        dept.setTreePath("0");  // 占位，insert 后更新
        deptMapper.insert(dept);

        // 计算真实 treePath
        String treePath = buildTreePath(req.getParentId(), dept.getId());
        dept.setTreePath(treePath);
        deptMapper.updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(String tenantId, DeptSaveRequest req) {
        SysDept existing = getDeptById(req.getId());
        existing.setName(req.getName());
        existing.setSort(req.getSort() != null ? req.getSort() : existing.getSort());
        existing.setLeader(req.getLeader());
        existing.setPhone(req.getPhone());
        existing.setEmail(req.getEmail());
        existing.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : existing.getStatus());
        deptMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(String id) {
        SysDept dept = getDeptById(id);
        long childCount = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getTenantId, dept.getTenantId())
                .eq(SysDept::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "请先删除子部门");
        }
        deptMapper.deleteById(id);
    }

    private String buildTreePath(String parentId, String currentId) {
        if ("0".equals(parentId)) {
            return "0," + currentId;
        }
        SysDept parent = deptMapper.selectById(parentId);
        if (parent == null) {
            return "0," + currentId;
        }
        return parent.getTreePath() + "," + currentId;
    }
}
