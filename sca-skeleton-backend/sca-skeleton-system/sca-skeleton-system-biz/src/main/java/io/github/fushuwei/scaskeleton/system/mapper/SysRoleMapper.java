package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.entity.SysRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色 Mapper。
 *
 * @author Fu Wei
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 分页查询角色，关联子查询一次性查出权限数量。
     *
     * @param page           分页对象
     * @param tenantId       租户ID
     * @param keyword        关键词（可选，模糊匹配名称或编码）
     * @param dataScope      数据权限范围（可选）
     * @param orderBy        排序字段（可选，白名单校验）
     * @param orderDirection 排序方向 ASC/DESC（可选）
     * @return 分页结果（每条记录含 permissionCount）
     */
    IPage<SysRole> selectRolePage(IPage<SysRole> page,
                                  @Param("tenantId") String tenantId,
                                  @Param("keyword") String keyword,
                                  @Param("dataScope") String dataScope,
                                  @Param("orderBy") String orderBy,
                                  @Param("orderDirection") String orderDirection);

    /**
     * 查询指定用户的角色列表。
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 角色列表
     */
    List<SysRole> selectRolesByUserId(@Param("userId") String userId,
                                      @Param("tenantId") String tenantId);
}
