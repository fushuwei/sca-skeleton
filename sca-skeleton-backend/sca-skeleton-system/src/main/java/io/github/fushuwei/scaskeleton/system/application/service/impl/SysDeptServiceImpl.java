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

    /** 部门主表 Mapper */
    private final SysDeptMapper deptMapper;

    @Override
    public List<SysDept> listDepts(String tenantId) {
        // 按租户查询全部部门，按 sort 升序
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getTenantId, tenantId)
                .orderByAsc(SysDept::getSort));
    }

    @Override
    public SysDept getDeptById(String id) {
        // 按主键查询部门
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            // 未命中则抛业务异常
            throw new BusinessException(ResultCode.NOT_FOUND, "部门不存在");
        }
        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDept(String tenantId, DeptSaveRequest req) {
        // 组装部门实体
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
        // 先插入以获取自增主键 ID
        deptMapper.insert(dept);

        // 计算真实 treePath 并回写
        String treePath = buildTreePath(req.getParentId(), dept.getId());
        dept.setTreePath(treePath);
        deptMapper.updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(String tenantId, DeptSaveRequest req) {
        // 校验部门存在并加载当前快照（parentId / treePath 不在此接口变更）
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
        // 加载待删部门
        SysDept dept = getDeptById(id);
        // 存在子部门时不允许删除
        long childCount = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getTenantId, dept.getTenantId())
                .eq(SysDept::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "请先删除子部门");
        }
        // 逻辑删除部门主表
        deptMapper.deleteById(id);
    }

    /**
     * 根据父节点 ID 与当前节点 ID 拼接树路径。
     *
     * @param parentId  父部门 ID，根节点为 "0"
     * @param currentId 当前部门 ID
     * @return 逗号分隔的树路径，如 {@code 0,parentId,currentId}
     */
    private String buildTreePath(String parentId, String currentId) {
        // 根节点下直接挂载
        if ("0".equals(parentId)) {
            return "0," + currentId;
        }
        // 父节点存在则继承其 treePath
        SysDept parent = deptMapper.selectById(parentId);
        if (parent == null) {
            // 父节点缺失时降级为根路径
            return "0," + currentId;
        }
        return parent.getTreePath() + "," + currentId;
    }
}
