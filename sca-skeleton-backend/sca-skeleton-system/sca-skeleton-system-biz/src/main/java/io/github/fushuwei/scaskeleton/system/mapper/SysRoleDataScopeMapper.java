package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import io.github.fushuwei.scaskeleton.system.entity.SysRoleDataScope;
import org.apache.ibatis.logging.LogFactory;

import java.util.Collection;

/**
 * 角色自定义数据权限（部门）关联 Mapper
 *
 * @author Fu Wei
 */
public interface SysRoleDataScopeMapper extends BaseMapper<SysRoleDataScope> {

    /**
     * 批量插入角色自定义数据权限关联
     * <p>
     * 复用 MyBatis-Plus 的 INSERT_ONE 语句，通过 JDBC Batch（默认每批 1000 条）批量执行，
     * 主键生成与 createBy/createTime 等审计字段自动填充均正常生效，
     * 相比循环逐条插入显著降低网络与 SQL 解析开销。
     *
     * @param entities 实体列表
     * @return 是否全部插入成功
     */
    default boolean insertBatch(Collection<SysRoleDataScope> entities) {
        String sqlStatement = SqlHelper.getSqlStatement(SysRoleDataScope.class, SqlMethod.INSERT_ONE);
        return SqlHelper.executeBatch(SysRoleDataScope.class, LogFactory.getLog(getClass()), entities, 1000,
            (sqlSession, entity) -> sqlSession.insert(sqlStatement, entity));
    }
}
