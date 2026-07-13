package io.github.fushuwei.scaskeleton.log.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import io.github.fushuwei.scaskeleton.log.entity.SysLoginLog;
import org.apache.ibatis.annotations.Param;

/**
 * 登录日志 Mapper
 *
 * @author Fu Wei
 */
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    /**
     * 分页查询登录日志（关联 sys_user 拼接操作人展示名称）
     *
     * @param page         分页对象
     * @param queryWrapper 查询条件（由 LambdaQueryWrapper 构建）
     * @return 分页结果
     */
    IPage<SysLoginLog> selectLogPage(IPage<SysLoginLog> page,
                                      @Param(Constants.WRAPPER) Wrapper<SysLoginLog> queryWrapper);

    /**
     * 按 ID 查询登录日志详情
     *
     * @param id 登录日志 ID
     * @return 登录日志对象
     */
    SysLoginLog selectLogById(@Param("id") String id);
}
