package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptResponse;

import java.util.List;

/**
 * 部门管理 Service
 *
 * @author Fu Wei
 */
public interface SysDeptService {

    /**
     * 查询部门列表
     *
     * @return 部门列表
     */
    List<DeptResponse> listDepts();

    /**
     * 查询部门选项列表
     *
     * @param tenantId 目标租户 ID（超管必传，未传返回空；非超管忽略，使用登录人所属的租户）
     * @return 部门选项列表
     */
    List<DeptOptionResponse> listDeptOptions(String tenantId);

    /**
     * 分页查询部门列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<DeptResponse> pageDepts(DeptPageRequest request);

    /**
     * 根据 ID 查询部门详情
     *
     * @param id 部门 ID
     * @return 部门详情
     */
    DeptResponse getDeptById(String id);

    /**
     * 新增部门
     *
     * @param request 部门信息
     */
    void createDept(DeptCreateRequest request);

    /**
     * 编辑部门
     *
     * @param request 部门信息
     */
    void updateDept(DeptUpdateRequest request);

    /**
     * 删除部门
     *
     * @param id 部门 ID
     */
    void deleteDept(String id);

    /**
     * 批量删除部门
     *
     * @param ids 部门 ID 列表
     */
    void batchDeleteDepts(List<String> ids);
}
