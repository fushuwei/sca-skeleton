package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptUpdateRequest;
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

    /** 分页查询指定父节点下的子部门列表（按租户隔离） */
    IPage<DeptResponse> pageDepts(String tenantId, DeptPageRequest request);

    DeptResponse getDeptById(String id);

    void createDept(String tenantId, DeptCreateRequest request);

    void updateDept(String tenantId, DeptUpdateRequest request);

    void deleteDept(String id);
}
