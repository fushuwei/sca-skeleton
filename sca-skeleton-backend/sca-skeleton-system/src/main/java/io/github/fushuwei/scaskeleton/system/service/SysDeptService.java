package io.github.fushuwei.scaskeleton.system.service;

import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptSaveRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptResponse;

import java.util.List;

/**
 * 部门管理服务接口。
 *
 * @author Fu Wei
 */
public interface SysDeptService {

    /** 查询租户下全量部门列表（前端自行构建树形结构） */
    List<DeptResponse> listDepts(String tenantId);

    DeptResponse getDeptById(String id);

    void createDept(String tenantId, DeptSaveRequest request);

    void updateDept(String tenantId, DeptSaveRequest request);

    void deleteDept(String id);
}
