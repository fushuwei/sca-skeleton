package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.system.entity.SysDept;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门 Mapper。
 *
 * @author Fu Wei
 */
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询指定节点的所有子部门ID（含自身）。
     * 用于删除部门时级联检查，或数据权限过滤。
     *
     * @param tenantId 租户ID
     * @param deptId   起始部门ID
     * @return 子部门 ID 列表（含 deptId 本身）
     */
    List<String> selectChildDeptIds(@Param("tenantId") String tenantId,
                                    @Param("deptId") String deptId);
}
