package io.github.fushuwei.scaskeleton.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.log.entity.SysOperationLog;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 操作日志 Mapper
 *
 * @author Fu Wei
 */
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 分页查询操作日志
     *
     * @param page           分页对象
     * @param module         操作模块（可选，模糊匹配）
     * @param action         操作动作（可选，模糊匹配）
     * @param operator       操作人（可选，模糊匹配）
     * @param isSuccess      操作状态（可选）
     * @param startTime      查询开始时间（可选）
     * @param endTime        查询结束时间（可选）
     * @param orderBy        排序字段（可选，白名单校验）
     * @param orderDirection 排序方向 ASC/DESC（可选）
     * @return 分页结果
     */
    IPage<SysOperationLog> selectLogPage(IPage<SysOperationLog> page,
                                         @Param("module") String module,
                                         @Param("action") String action,
                                         @Param("operator") String operator,
                                         @Param("isSuccess") Integer isSuccess,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         @Param("orderBy") String orderBy,
                                         @Param("orderDirection") String orderDirection);

    /**
     * 按 ID 查询操作日志详情
     *
     * @param id 操作日志 ID
     * @return 操作日志对象
     */
    SysOperationLog selectLogById(@Param("id") String id);
}
