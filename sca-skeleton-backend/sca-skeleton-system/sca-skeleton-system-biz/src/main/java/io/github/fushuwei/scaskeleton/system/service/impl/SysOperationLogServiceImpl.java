package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.log.entity.SysOperationLog;
import io.github.fushuwei.scaskeleton.log.mapper.SysOperationLogMapper;
import io.github.fushuwei.scaskeleton.system.api.request.operationlog.OperationLogPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.operationlog.OperationLogResponse;
import io.github.fushuwei.scaskeleton.system.converter.OperationLogConverter;
import io.github.fushuwei.scaskeleton.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 操作日志服务实现。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperationLogServiceImpl implements SysOperationLogService {

    private final SysOperationLogMapper operationLogMapper;
    private final OperationLogConverter operationLogConverter;

    @Override
    public IPage<OperationLogResponse> pageLogs(OperationLogPageRequest req) {
        Page<SysOperationLog> page = new Page<>(req.getPageNum(), req.getPageSize());

        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<SysOperationLog>()
            .like(StringUtils.hasText(req.getModule()), SysOperationLog::getModule, req.getModule())
            .like(StringUtils.hasText(req.getAction()), SysOperationLog::getAction, req.getAction())
            .apply(StringUtils.hasText(req.getOperator()),
                   "CONCAT_WS(' ', u.real_name, CONCAT('(', u.username, ')')) LIKE CONCAT('%', {0}, '%')",
                   req.getOperator())
            .eq(req.getIsSuccess() != null, SysOperationLog::getIsSuccess, req.getIsSuccess())
            .ge(req.getStartTime() != null, SysOperationLog::getOperationTime, req.getStartTime())
            .le(req.getEndTime() != null, SysOperationLog::getOperationTime, req.getEndTime());

        // 动态排序
        String orderBy = req.safeOrderBy();
        boolean isAsc = "ASC".equalsIgnoreCase(req.safeOrderDirection());
        if (orderBy != null) {
            switch (orderBy) {
                case "operation_time" -> wrapper.orderBy(true, isAsc, SysOperationLog::getOperationTime);
                case "operator" -> wrapper.orderBy(true, isAsc, SysOperationLog::getOperator);
                case "module" -> wrapper.orderBy(true, isAsc, SysOperationLog::getModule);
                case "action" -> wrapper.orderBy(true, isAsc, SysOperationLog::getAction);
                case "http_method" -> wrapper.orderBy(true, isAsc, SysOperationLog::getHttpMethod);
                case "request_uri" -> wrapper.orderBy(true, isAsc, SysOperationLog::getRequestUri);
                case "client_ip" -> wrapper.orderBy(true, isAsc, SysOperationLog::getClientIp);
                case "cost_ms" -> wrapper.orderBy(true, isAsc, SysOperationLog::getCostMs);
                case "is_success" -> wrapper.orderBy(true, isAsc, SysOperationLog::getIsSuccess);
            }
        } else {
            wrapper.orderByDesc(SysOperationLog::getOperationTime);
        }

        IPage<SysOperationLog> entityPage = operationLogMapper.selectLogPage(page, wrapper);
        return entityPage.convert(operationLogConverter::toOperationLogResponse);
    }

    @Override
    public OperationLogResponse getLogById(String id) {
        SysOperationLog logEntity = operationLogMapper.selectLogById(id);
        if (logEntity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日志不存在");
        }
        return operationLogConverter.toOperationLogResponse(logEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteLogs(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        operationLogMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllLogs() {
        operationLogMapper.delete(new LambdaQueryWrapper<>());
    }
}
