package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import io.github.fushuwei.scaskeleton.system.entity.SysRoleDept;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 角色部门关联 Mapper
 *
 * @author Fu Wei
 */
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {

    /**
     * 批量插入角色部门关联
     * <p>
     * 通过 {@link Db#saveBatch(Collection)} 复用 MyBatis-Plus 的 INSERT_ONE 语句（statement id
     * 由框架基于 Mapper 接口命名空间自动生成，即 {@code <Mapper 接口全限定名>.insert}，与框架注入
     * 的语句完全一致），以 JDBC Batch 方式执行，默认每批 1000 条。
     * 主键生成（ASSIGN_UUID）与 createBy/createTime 等审计字段自动填充均正常生效，
     * 相比循环逐条插入显著降低网络与 SQL 解析开销。
     *
     * @param entities 实体列表
     * @return 是否全部插入成功
     */
    default boolean insertBatch(Collection<SysRoleDept> entities) {
        return Db.saveBatch(entities);
    }

    /**
     * 物理删除角色与部门的关联关系
     * <p>
     * 角色-部门为"编辑角色时全量重建"的关联数据，删除后不再查询历史，故直接物理删除
     * （不走逻辑删除），避免 is_deleted=1 的脏行累积。表结构保持不变。
     *
     * @param roleIds 角色 ID 列表
     * @return 删除行数
     */
    @Delete("<script>DELETE FROM sys_role_dept WHERE role_id IN "
        + "<foreach collection='roleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int physicalDeleteByRoleIds(@Param("roleIds") List<String> roleIds);
}
