package io.github.fushuwei.scaskeleton.log.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import io.github.fushuwei.scaskeleton.log.entity.SysOperationLog;
import org.apache.ibatis.annotations.Param;

/**
 * 操作日志 Mapper
 *
 * @author Fu Wei
 */
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 分页查询操作日志
     *
     * @param page         分页对象
     * @param queryWrapper 查询条件（由 LambdaQueryWrapper 构建）
     * @return 分页结果
     */
    IPage<SysOperationLog> selectLogPage(IPage<SysOperationLog> page,
                                         @Param(Constants.WRAPPER) Wrapper<SysOperationLog> queryWrapper);

    /**
     * 按 ID 查询操作日志详情
     *
     * @param id 操作日志 ID
     * @return 操作日志对象
     */
    SysOperationLog selectLogById(@Param("id") String id);
}
