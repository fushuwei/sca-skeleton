package io.github.fushuwei.scaskeleton.logging.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import io.github.fushuwei.scaskeleton.logging.entity.SysOperationLog;
import org.apache.ibatis.annotations.Param;

/**
 * 操作日志基础 Mapper。
 * <p>
 * 提供 {@link BaseMapper} 内置的 insert、delete 等方法，满足所有微服务的异步持久化需求。
 *
 * @author Fu Wei
 */
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 分页查询操作日志（LEFT JOIN sys_user 获取操作人展示名称）。
     *
     * @param page         分页对象
     * @param queryWrapper 查询条件（由 LambdaQueryWrapper 构建）
     * @return 分页结果（含操作人展示名称）
     */
    IPage<SysOperationLog> selectLogPage(IPage<SysOperationLog> page,
                                         @Param(Constants.WRAPPER) Wrapper<SysOperationLog> queryWrapper);

    /**
     * 按 ID 查询操作日志详情（LEFT JOIN sys_user 获取操作人展示名称）。
     *
     * @param id 日志 ID
     * @return 操作日志（含操作人展示名称）
     */
    SysOperationLog selectLogById(@Param("id") String id);
}
