package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.log.entity.SysLoginLog;
import io.github.fushuwei.scaskeleton.log.mapper.SysLoginLogMapper;
import io.github.fushuwei.scaskeleton.system.api.request.loginlog.LoginLogPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.loginlog.LoginLogResponse;
import io.github.fushuwei.scaskeleton.system.converter.LoginLogConverter;
import io.github.fushuwei.scaskeleton.system.service.SysLoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 登录日志服务实现。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLoginLogServiceImpl implements SysLoginLogService {

    private final SysLoginLogMapper loginLogMapper;
    private final LoginLogConverter loginLogConverter;

    @Override
    public IPage<LoginLogResponse> pageLogs(LoginLogPageRequest req) {
        Page<SysLoginLog> page = new Page<>(req.getPageNum(), req.getPageSize());
        IPage<SysLoginLog> entityPage = loginLogMapper.selectLogPage(page,
                req.getKeyword(),
                req.getIsSuccess(),
                req.getStartTime(),
                req.getEndTime(),
                req.safeOrderBy(),
                req.safeOrderDirection());
        return entityPage.convert(loginLogConverter::toLoginLogResponse);
    }

    @Override
    public LoginLogResponse getLogById(String id) {
        SysLoginLog logEntity = loginLogMapper.selectLogById(id);
        if (logEntity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日志不存在");
        }
        return loginLogConverter.toLoginLogResponse(logEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteLogs(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        loginLogMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllLogs() {
        loginLogMapper.delete(null);
    }
}
