package io.github.fushuwei.scaskeleton.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.log.entity.SysLoginLog;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 登录日志管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    /**
     * 分页查询登录日志列表
     *
     * @param page      分页对象
     * @param tenantId  租户 ID
     * @param keyword   搜索关键字
     * @param isSuccess 是否成功
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param sortField 排序字段
     * @param sortOrder 排序方向
     * @return 分页结果
     */
    IPage<SysLoginLog> selectLogPage(IPage<SysLoginLog> page,
                                     @Param("tenantId") String tenantId,
                                     @Param("keyword") String keyword,
                                     @Param("isSuccess") Integer isSuccess,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     @Param("sortField") String sortField,
                                     @Param("sortOrder") String sortOrder);

    /**
     * 根据 ID 查询登录日志详情
     *
     * @param id 登录日志 ID
     * @return 登录日志对象
     */
    SysLoginLog selectLogById(@Param("id") String id);

    /**
     * 清空全部登录日志
     *
     * @return 受影响行数
     */
    int clearAllLogs();
}
