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
     * 批量更新所有子孙节点的 tree_path 字段值
     *
     * @param tenantId     租户 ID
     * @param oldPrefix    变更前的 treePath 前缀
     * @param newPrefix    变更后的 treePath 前缀
     * @param oldPrefixLen 旧前缀的字符长度
     */
    void updateDescendantsTreePath(@Param("tenantId") String tenantId,
                                   @Param("oldPrefix") String oldPrefix,
                                   @Param("newPrefix") String newPrefix,
                                   @Param("oldPrefixLen") int oldPrefixLen);
}
