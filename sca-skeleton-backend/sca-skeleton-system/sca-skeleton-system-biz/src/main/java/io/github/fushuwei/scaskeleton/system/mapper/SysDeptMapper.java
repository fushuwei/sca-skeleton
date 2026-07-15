package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
     * 查询指定节点的所有子部门 ID（含自身）
     *
     * @param tenantId 租户 ID
     * @param deptId   部门 ID
     * @return 子部门 ID 列表（含自身）
     */
    List<String> selectChildDeptIds(@Param("tenantId") String tenantId, @Param("deptId") String deptId);

    /**
     * 批量更新所有子孙节点的 tree_path 字段值
     *
     * @param tenantId    租户 ID
     * @param oldTreePath 变更前的 treePath
     * @param newTreePath 变更后的 treePath
     */
    void updateDescendantsTreePath(@Param("tenantId") String tenantId,
                                   @Param("oldTreePath") String oldTreePath,
                                   @Param("newTreePath") String newTreePath);
}
