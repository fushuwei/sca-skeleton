package io.github.fushuwei.scaskeleton.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.log.entity.SysOperationLog;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 操作日志管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 分页查询操作日志列表
     *
     * @param page      分页对象
     * @param tenantId  租户 ID
     * @param keyword   搜索关键字（模糊匹配租户名称、操作用户、真实姓名、操作模块、操作动作、请求路径）
     * @param isSuccess 操作状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param sortField 排序字段
     * @param sortOrder 排序方向
     * @return 分页结果
     */
    IPage<SysOperationLog> selectLogPage(IPage<SysOperationLog> page,
                                         @Param("tenantId") String tenantId,
                                         @Param("keyword") String keyword,
                                         @Param("isSuccess") Integer isSuccess,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         @Param("sortField") String sortField,
                                         @Param("sortOrder") String sortOrder);

    /**
     * 根据 ID 查询操作日志详情
     *
     * @param id 操作日志 ID
     * @return 操作日志对象
     */
    SysOperationLog selectLogById(@Param("id") String id);

    /**
     * 清空全部操作日志
     *
     * @return 受影响行数
     */
    int clearAllLogs();
}
