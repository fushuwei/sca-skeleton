package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.system.api.request.operationlog.OperationLogPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.operationlog.OperationLogResponse;
import io.github.fushuwei.scaskeleton.system.converter.OperationLogConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysOperationLog;
import io.github.fushuwei.scaskeleton.system.mapper.SysOperationLogMapper;
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
                .eq(StringUtils.hasText(req.getModule()), SysOperationLog::getModule, req.getModule())
                .eq(StringUtils.hasText(req.getAction()), SysOperationLog::getAction, req.getAction())
                .like(StringUtils.hasText(req.getUsername()), SysOperationLog::getUsername, req.getUsername())
                .eq(req.getSuccess() != null, SysOperationLog::getSuccess, req.getSuccess())
                .ge(req.getStartTime() != null, SysOperationLog::getOperationTime, req.getStartTime())
                .le(req.getEndTime() != null, SysOperationLog::getOperationTime, req.getEndTime())
                .orderByDesc(SysOperationLog::getOperationTime);

        IPage<SysOperationLog> entityPage = operationLogMapper.selectPage(page, wrapper);
        return entityPage.convert(operationLogConverter::toOperationLogResponse);
    }

    @Override
    public OperationLogResponse getLogById(String id) {
        SysOperationLog logEntity = operationLogMapper.selectById(id);
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
