package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.log.entity.SysOperationLog;
import io.github.fushuwei.scaskeleton.log.mapper.SysOperationLogMapper;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.operationlog.OperationLogPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.operationlog.OperationLogResponse;
import io.github.fushuwei.scaskeleton.system.converter.OperationLogConverter;
import io.github.fushuwei.scaskeleton.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 操作日志管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperationLogServiceImpl implements SysOperationLogService {

    private final SysOperationLogMapper operationLogMapper;

    private final OperationLogConverter operationLogConverter;

    /**
     * 分页查询操作日志
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<OperationLogResponse> pageLogs(OperationLogPageRequest request) {
        // 构造分页对象
        Page<SysOperationLog> page = new Page<>(request.getPageNum(), request.getPageSize());
        // 查询分页数据，并将结果转换为响应对象
        IPage<SysOperationLog> entityPage = operationLogMapper.selectLogPage(page,
            SecurityUtils.getTenantId(),  // 数据隔离：仅查询当前租户下的操作日志
            request.getKeyword(),
            request.getIsSuccess(),
            request.getStartTime(),
            request.getEndTime(),
            request.safeSortField(),
            request.safeSortOrder());
        return entityPage.convert(operationLogConverter::toOperationLogResponse);
    }

    /**
     * 根据 ID 查询操作日志详情
     *
     * @param id 操作日志 ID
     * @return 操作日志详情
     */
    @Override
    public OperationLogResponse getLogById(String id) {
        // 按主键查询当前租户下的日志
        SysOperationLog logEntity = operationLogMapper.selectLogById(id, SecurityUtils.getTenantId());
        if (logEntity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日志不存在");
        }
        // 转换为响应对象
        return operationLogConverter.toOperationLogResponse(logEntity);
    }

    /**
     * 批量删除操作日志
     *
     * @param ids 操作日志 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteLogs(List<String> ids) {
        // 仅超级管理员可批量删除操作日志
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可批量删除操作日志");
        }
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        operationLogMapper.deleteBatchIds(ids);
    }

    /**
     * 清空全部操作日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllLogs() {
        // 仅超级管理员可清空操作日志
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可清空操作日志");
        }
        operationLogMapper.clearAllLogs();
    }
}
