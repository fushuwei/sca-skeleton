package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.system.entity.SysPermission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询指定角色的权限列表
     *
     * @param roleIds 角色 ID 列表
     * @return 权限列表（去重）
     */
    List<SysPermission> selectPermissionsByRoleIds(@Param("roleIds") List<String> roleIds);

    /**
     * 批量更新所有子孙节点的 tree_path 字段值
     *
     * @param oldPrefix    变更前的 treePath 前缀
     * @param newPrefix    变更后的 treePath 前缀
     * @param oldPrefixLen 旧前缀的字符长度
     */
    void updateDescendantsTreePath(@Param("oldPrefix") String oldPrefix, @Param("newPrefix") String newPrefix, @Param("oldPrefixLen") int oldPrefixLen);
}
