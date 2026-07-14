package io.github.fushuwei.scaskeleton.system.service.impl;

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
        IPage<SysOperationLog> entityPage = operationLogMapper.selectLogPage(page,
                req.getModule(),
                req.getAction(),
                req.getOperator(),
                req.getIsSuccess(),
                req.getStartTime(),
                req.getEndTime(),
                req.safeOrderBy(),
                req.safeOrderDirection());
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
        operationLogMapper.delete(null);
    }
}
