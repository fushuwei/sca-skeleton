package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import io.github.fushuwei.scaskeleton.system.entity.SysRoleDept;

import java.util.Collection;

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
}
