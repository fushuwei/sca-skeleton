package io.github.fushuwei.scaskeleton.system.application.service;

import io.github.fushuwei.scaskeleton.system.api.dto.dept.DeptSaveRequest;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysDept;

import java.util.List;

/**
 * 部门管理服务接口。
 *
 * @author Fu Wei
 */
public interface SysDeptService {

    /** 查询租户下全量部门列表（前端自行构建树形结构） */
    List<SysDept> listDepts(String tenantId);

    SysDept getDeptById(String id);

    void createDept(String tenantId, DeptSaveRequest request);

    void updateDept(String tenantId, DeptSaveRequest request);

    void deleteDept(String id);
}
