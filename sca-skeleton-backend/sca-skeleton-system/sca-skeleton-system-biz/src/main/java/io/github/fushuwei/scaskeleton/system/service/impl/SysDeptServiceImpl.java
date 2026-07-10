package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptResponse;
import io.github.fushuwei.scaskeleton.system.converter.DeptConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysDept;
import io.github.fushuwei.scaskeleton.system.mapper.SysDeptMapper;
import io.github.fushuwei.scaskeleton.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    /** Entity ↔ Response 转换器（MapStruct 生成） */
    private final DeptConverter deptConverter;

    @Override
    public List<DeptResponse> listDepts(String tenantId) {
        // 按租户查询全部部门，按 sort 升序
        List<SysDept> depts = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getTenantId, tenantId)
                .orderByAsc(SysDept::getSort));
        // 转换为响应对象列表
        return depts.stream().map(deptConverter::toDeptResponse).toList();
    }

    @Override
    public IPage<DeptResponse> pageDepts(String tenantId, DeptPageRequest req) {
        // 构造分页对象
        Page<SysDept> page = new Page<>(req.getPageNum(), req.getPageSize());

        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<SysDept>()
                // 按租户隔离
                .eq(SysDept::getTenantId, tenantId)
                // 按父节点筛选子部门，parentId 为空时不按父节点过滤（返回全部记录）
                .eq(StringUtils.hasText(req.getParentId()), SysDept::getParentId, req.getParentId())
                // 关键词模糊匹配名称或编码
                .and(StringUtils.hasText(req.getKeyword()),
                        w -> w.like(SysDept::getName, req.getKeyword())
                                .or().like(SysDept::getCode, req.getKeyword()))
                // 状态筛选
                .eq(StringUtils.hasText(req.getStatus()), SysDept::getStatus, req.getStatus());

        // 安全排序：白名单校验通过后按指定字段排序，否则按 sort 升序
        String orderBy = req.safeOrderBy();
        boolean isAsc = "ASC".equalsIgnoreCase(req.safeOrderDirection());
        if (orderBy != null) {
            switch (orderBy) {
                case "name" -> wrapper.orderBy(true, isAsc, SysDept::getName);
                case "code" -> wrapper.orderBy(true, isAsc, SysDept::getCode);
                case "sort" -> wrapper.orderBy(true, isAsc, SysDept::getSort);
                case "status" -> wrapper.orderBy(true, isAsc, SysDept::getStatus);
                case "create_time" -> wrapper.orderBy(true, isAsc, SysDept::getCreateTime);
                case "tree_path" -> wrapper.orderBy(true, isAsc, SysDept::getTreePath);
            }
        } else {
            wrapper.orderByAsc(SysDept::getSort);
        }

        // 查询实体分页并转换为响应对象分页
        IPage<SysDept> entityPage = deptMapper.selectPage(page, wrapper);
        return entityPage.convert(deptConverter::toDeptResponse);
    }

    @Override
    public DeptResponse getDeptById(String id) {
        // 按主键查询部门并转换为响应对象
        return deptConverter.toDeptResponse(loadDeptEntity(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDept(String tenantId, DeptCreateRequest req) {
        // 部门编码在同租户内唯一
        long count = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getTenantId, tenantId)
                .eq(SysDept::getCode, req.getCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "部门编码已存在");
        }
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
    public void updateDept(String tenantId, DeptUpdateRequest req) {
        // 校验部门存在并加载当前快照
        SysDept existing = loadDeptEntity(req.getId());

        boolean parentChanged = false;
        String oldParentId = existing.getParentId();

        // 处理上级部门变更
        if (StringUtils.hasText(req.getParentId()) && !req.getParentId().equals(existing.getParentId())) {
            String newParentId = req.getParentId();

            // 校验新上级部门不能是自己
            if (newParentId.equals(existing.getId())) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "上级部门不能选择自己");
            }

            // 校验新上级部门不能是自己的下级部门（含递归）
            if (!"0".equals(newParentId)) {
                List<String> descendantIds = getDescendantIds(existing.getId());
                if (descendantIds.contains(newParentId)) {
                    throw new BusinessException(ResultCode.VALIDATION_ERROR, "上级部门不能选择自己的下级部门");
                }
                // 校验新上级部门存在
                loadDeptEntity(newParentId);
            }

            existing.setParentId(newParentId);
            parentChanged = true;
        }

        // 编码变更时校验同租户内唯一（排除自身）
        if (StringUtils.hasText(req.getCode()) && !req.getCode().equals(existing.getCode())) {
            long count = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                    .eq(SysDept::getTenantId, tenantId)
                    .eq(SysDept::getCode, req.getCode())
                    .ne(SysDept::getId, req.getId()));
            if (count > 0) {
                throw new BusinessException(ResultCode.ALREADY_EXISTS, "部门编码已存在");
            }
            existing.setCode(req.getCode());
        }

        // 更新其他字段
        existing.setName(req.getName());
        existing.setSort(req.getSort() != null ? req.getSort() : existing.getSort());
        existing.setLeader(req.getLeader());
        existing.setPhone(req.getPhone());
        existing.setEmail(req.getEmail());
        existing.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : existing.getStatus());

        // 如果上级部门变更，重新计算 treePath
        if (parentChanged) {
            String newTreePath = buildTreePath(existing.getParentId(), existing.getId());
            existing.setTreePath(newTreePath);
        }

        deptMapper.updateById(existing);

        // 上级部门变更后，递归更新所有子孙部门的 treePath
        if (parentChanged) {
            updateDescendantsTreePath(existing);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(String id) {
        // 加载待删部门
        SysDept dept = loadDeptEntity(id);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDepts(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        for (String id : ids) {
            deleteDept(id);
        }
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

    /**
     * 获取指定部门的所有子孙部门 ID 列表（含子、孙、曾孙等递归下级）。
     *
     * @param parentId 父部门 ID
     * @return 子孙部门 ID 列表
     */
    private List<String> getDescendantIds(String parentId) {
        List<String> result = new ArrayList<>();
        List<SysDept> children = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, parentId));
        for (SysDept child : children) {
            result.add(child.getId());
            result.addAll(getDescendantIds(child.getId()));
        }
        return result;
    }

    /**
     * 递归更新所有子孙部门的 treePath。
     *
     * @param parent 父部门实体（已更新 treePath）
     */
    private void updateDescendantsTreePath(SysDept parent) {
        List<SysDept> children = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, parent.getId()));
        for (SysDept child : children) {
            child.setTreePath(parent.getTreePath() + "," + child.getId());
            deptMapper.updateById(child);
            updateDescendantsTreePath(child);
        }
    }

    /**
     * 按主键加载部门实体（供内部业务逻辑使用，不对外暴露 Entity）。
     *
     * @param id 部门 ID
     * @return 部门实体
     * @throws BusinessException 部门不存在时抛出 NOT_FOUND
     */
    private SysDept loadDeptEntity(String id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "部门不存在");
        }
        return dept;
    }
}
