package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptResponse;
import io.github.fushuwei.scaskeleton.system.converter.DeptConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysDept;
import io.github.fushuwei.scaskeleton.system.mapper.SysDeptMapper;
import io.github.fushuwei.scaskeleton.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 部门管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl implements SysDeptService {

    private final SysDeptMapper deptMapper;

    private final DeptConverter deptConverter;

    /**
     * 查询部门列表
     *
     * @return 部门列表
     */
    @Override
    public List<DeptResponse> listDepts() {
        // 查询当前租户下的全部部门
        List<SysDept> depts = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
            .eq(SysDept::getTenantId, SecurityUtils.getTenantId())
            .orderByAsc(SysDept::getSort));
        // 转换为响应对象列表
        return depts.stream().map(deptConverter::toDeptResponse).toList();
    }

    /**
     * 分页查询部门列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<DeptResponse> pageDepts(DeptPageRequest request) {
        // 构造分页对象
        Page<SysDept> page = new Page<>(request.getPageNum(), request.getPageSize());

        // 包装查询条件
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<SysDept>()
            .eq(SysDept::getTenantId, SecurityUtils.getTenantId())
            .eq(StringUtils.hasText(request.getParentId()), SysDept::getParentId, request.getParentId())
            .and(StringUtils.hasText(request.getKeyword()),
                w -> w.like(SysDept::getName, request.getKeyword()).or().like(SysDept::getCode, request.getKeyword()))
            .eq(StringUtils.hasText(request.getStatus()), SysDept::getStatus, request.getStatus());

        // 包装排序规则
        String sortField = request.safeSortField();
        boolean isAsc = "ASC".equalsIgnoreCase(request.safeSortOrder());
        if (sortField != null) {
            switch (sortField) {
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

        // 查询分页数据，并将结果转换为响应对象
        IPage<SysDept> entityPage = deptMapper.selectPage(page, wrapper);
        return entityPage.convert(deptConverter::toDeptResponse);
    }

    /**
     * 根据 ID 查询部门详情
     *
     * @param id 部门 ID
     * @return 部门详情
     */
    @Override
    public DeptResponse getDeptById(String id) {
        return deptConverter.toDeptResponse(loadDeptEntity(id));
    }

    /**
     * 新增部门
     *
     * @param request 部门信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDept(DeptCreateRequest request) {
        // 获取租户 ID
        String tenantId = SecurityUtils.getTenantId();

        // 部门编码在同一个租户内唯一
        long count = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
            .eq(SysDept::getTenantId, tenantId)
            .eq(SysDept::getCode, request.getCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "部门编码已存在");
        }

        // 封装部门实体
        SysDept dept = new SysDept();
        dept.setId(UuidUtils.nextSimpleStr());
        dept.setTenantId(tenantId);
        dept.setParentId(request.getParentId());
        dept.setName(request.getName());
        dept.setCode(request.getCode());
        dept.setSort(request.getSort() != null ? request.getSort() : 100);
        dept.setLeader(request.getLeader());
        dept.setPhone(request.getPhone());
        dept.setEmail(request.getEmail());
        dept.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "enabled");
        dept.setTreePath(buildTreePath(dept.getParentId(), dept.getId()));

        // 保存部门
        deptMapper.insert(dept);
    }

    /**
     * 编辑部门
     *
     * @param request 部门信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(DeptUpdateRequest request) {
        // 加载部门实体
        SysDept dept = loadDeptEntity(request.getId());

        // 保存旧 treePath（用于批量更新子孙节点）
        String oldTreePath = dept.getTreePath();

        // 处理上级部门变更
        boolean parentChanged = false;
        if (StringUtils.hasText(request.getParentId()) && !request.getParentId().equals(dept.getParentId())) {
            String newParentId = request.getParentId();

            // 上级部门不能是自己
            if (newParentId.equals(dept.getId())) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "上级部门不能选择自己");
            }

            // 上级部门不能是自己的下级部门
            if (!"0".equals(newParentId)) {
                SysDept newParent = loadDeptEntity(newParentId);
                if (newParent.getTreePath().startsWith(dept.getTreePath() + ",")) {
                    throw new BusinessException(ResultCode.VALIDATION_ERROR, "上级部门不能选择自己的下级部门");
                }
            }

            dept.setParentId(newParentId);
            parentChanged = true;
        }

        // 部门编码在同一个租户内唯一（排除自身）
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(dept.getCode())) {
            long codeCount = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getTenantId, dept.getTenantId())
                .eq(SysDept::getCode, request.getCode())
                .ne(SysDept::getId, request.getId()));
            if (codeCount > 0) {
                throw new BusinessException(ResultCode.ALREADY_EXISTS, "部门编码已存在");
            }
            dept.setCode(request.getCode());
        }

        // 更新其他字段
        dept.setName(request.getName());
        dept.setSort(request.getSort() != null ? request.getSort() : dept.getSort());
        dept.setLeader(request.getLeader());
        dept.setPhone(request.getPhone());
        dept.setEmail(request.getEmail());
        dept.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : dept.getStatus());

        // 如果上级部门变更，重新生成 treePath
        if (parentChanged) {
            dept.setTreePath(buildTreePath(dept.getParentId(), dept.getId()));
        }

        // 更新部门
        deptMapper.updateById(dept);

        // 上级部门变更后，批量更新所有子孙节点的 tree_path 字段值
        if (parentChanged) {
            deptMapper.updateDescendantsTreePath(dept.getTenantId(), oldTreePath, dept.getTreePath(), oldTreePath.length() + 1);
        }
    }

    /**
     * 删除部门
     *
     * @param id 部门 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(String id) {
        // 加载部门实体
        SysDept dept = loadDeptEntity(id);

        // 存在子部门时不允许删除
        long childCount = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
            .eq(SysDept::getTenantId, dept.getTenantId())
            .eq(SysDept::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "请先删除子部门");
        }

        // 删除部门
        deptMapper.deleteById(id);
    }

    /**
     * 批量删除部门
     *
     * @param ids 部门 ID 列表
     */
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
     * 生成当前节点的树路径
     *
     * @param parentId  父部门 ID
     * @param currentId 当前部门 ID
     * @return 逗号分隔的树路径
     */
    private String buildTreePath(String parentId, String currentId) {
        // 根节点下直接挂载
        if ("0".equals(parentId)) {
            return "0," + currentId;
        }
        // 父节点存在则继承其 treePath
        SysDept parent = deptMapper.selectById(parentId);
        if (parent == null) {
            return "0," + currentId;
        }
        return parent.getTreePath() + "," + currentId;
    }

    /**
     * 根据 ID 加载部门实体
     *
     * @param id 部门 ID
     * @return 部门实体
     */
    private SysDept loadDeptEntity(String id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "部门不存在");
        }
        if (!SecurityUtils.isSuperAdmin()
            && !Objects.equals(dept.getTenantId(), SecurityUtils.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
        }
        return dept;
    }
}
