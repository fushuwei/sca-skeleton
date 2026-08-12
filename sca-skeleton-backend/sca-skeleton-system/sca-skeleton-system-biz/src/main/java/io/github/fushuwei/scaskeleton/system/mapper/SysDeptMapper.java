package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysDept;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 分页查询部门列表
     *
     * @param page     分页对象
     * @param tenantId 租户 ID
     * @param request  查询条件
     * @return 分页结果
     */
    IPage<DeptResponse> selectDeptPage(IPage<DeptResponse> page, @Param("tenantId") String tenantId, @Param("request") DeptPageRequest request);

    /**
     * 查询部门选项列表
     * <p>
     * 单条 SQL 实现：查询部门属性（id、parentId、name、sort）并通过 tree_path 前缀匹配
     * 关联 sys_dept 与 sys_user_dept，一次聚合出各部门（含子部门）的去重用户数，避免 N+1 查询。
     *
     * @param tenantId 租户 ID
     * @return 部门选项列表（含各属性与 userCount）
     */
    List<DeptOptionResponse> selectDeptOptions(@Param("tenantId") String tenantId);

    /**
     * 批量更新所有子孙节点的 tree_path 字段值
     *
     * @param tenantId     租户 ID
     * @param oldPrefix    变更前的 treePath 前缀
     * @param newPrefix    变更后的 treePath 前缀
     * @param oldPrefixLen 旧前缀的字符长度
     */
    void updateDescendantsTreePath(@Param("tenantId") String tenantId, @Param("oldPrefix") String oldPrefix, @Param("newPrefix") String newPrefix, @Param("oldPrefixLen") int oldPrefixLen);
}
