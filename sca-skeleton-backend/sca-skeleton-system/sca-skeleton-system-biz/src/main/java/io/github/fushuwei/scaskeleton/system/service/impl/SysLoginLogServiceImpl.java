package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.util.StringUtils;

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

        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<SysLoginLog>()
            .like(StringUtils.hasText(req.getUsername()), SysLoginLog::getUsername, req.getUsername())
            .eq(req.getIsSuccess() != null, SysLoginLog::getIsSuccess, req.getIsSuccess())
            .like(StringUtils.hasText(req.getClientIp()), SysLoginLog::getClientIp, req.getClientIp())
            .ge(req.getStartTime() != null, SysLoginLog::getLoginTime, req.getStartTime())
            .le(req.getEndTime() != null, SysLoginLog::getLoginTime, req.getEndTime());

        // 动态排序
        String orderBy = req.safeOrderBy();
        boolean isAsc = "ASC".equalsIgnoreCase(req.safeOrderDirection());
        if (orderBy != null) {
            switch (orderBy) {
                case "login_time" -> wrapper.orderBy(true, isAsc, SysLoginLog::getLoginTime);
                case "username" -> wrapper.orderBy(true, isAsc, SysLoginLog::getUsername);
                case "client_ip" -> wrapper.orderBy(true, isAsc, SysLoginLog::getClientIp);
                case "is_success" -> wrapper.orderBy(true, isAsc, SysLoginLog::getIsSuccess);
                case "device" -> wrapper.orderBy(true, isAsc, SysLoginLog::getDevice);
                case "browser" -> wrapper.orderBy(true, isAsc, SysLoginLog::getBrowser);
                case "os" -> wrapper.orderBy(true, isAsc, SysLoginLog::getOs);
                case "cost_ms" -> wrapper.orderBy(true, isAsc, SysLoginLog::getCostMs);
            }
        } else {
            wrapper.orderByDesc(SysLoginLog::getLoginTime);
        }

        IPage<SysLoginLog> entityPage = loginLogMapper.selectLogPage(page, wrapper);
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
        loginLogMapper.delete(new LambdaQueryWrapper<>());
    }
}
