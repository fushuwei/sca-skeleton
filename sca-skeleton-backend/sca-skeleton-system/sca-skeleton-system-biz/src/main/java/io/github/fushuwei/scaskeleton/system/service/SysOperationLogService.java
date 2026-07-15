package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.operationlog.OperationLogPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.operationlog.OperationLogResponse;

import java.util.List;

/**
 * 操作日志管理 Service
 *
 * @author Fu Wei
 */
public interface SysOperationLogService {

    /**
     * 分页查询操作日志
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<OperationLogResponse> pageLogs(OperationLogPageRequest request);

    /**
     * 根据 ID 查询操作日志详情
     *
     * @param id 操作日志 ID
     * @return 操作日志详情
     */
    OperationLogResponse getLogById(String id);

    /**
     * 批量删除操作日志
     *
     * @param ids 操作日志 ID 列表
     */
    void batchDeleteLogs(List<String> ids);

    /**
     * 清空全部操作日志
     */
    void clearAllLogs();
}
