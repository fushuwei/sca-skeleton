package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

        // 使用 QueryWrapper 并加表别名前缀，避免 LEFT JOIN 后 username 等共享列名歧义
        QueryWrapper<SysLoginLog> wrapper = new QueryWrapper<>();
        wrapper.eq(req.getIsSuccess() != null, "l.is_success", req.getIsSuccess())
            .ge(req.getStartTime() != null, "l.login_time", req.getStartTime())
            .le(req.getEndTime() != null, "l.login_time", req.getEndTime());

        // 关键字模糊搜索：租户名称、登录用户、真实姓名、客户端 IP（OR 分组）
        if (StringUtils.hasText(req.getKeyword())) {
            String kw = req.getKeyword();
            wrapper.and(w -> w.like("l.username", kw)
                .or().like("l.client_ip", kw)
                .or().like("u.real_name", kw)
                .or().like("t.name", kw));
        }

        // 动态排序（列名加 l. 前缀避免歧义）
        String orderBy = req.safeOrderBy();
        boolean isAsc = "ASC".equalsIgnoreCase(req.safeOrderDirection());
        if (orderBy != null) {
            wrapper.orderBy(true, isAsc, "l." + orderBy);
        } else {
            wrapper.orderByDesc("l.login_time");
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
        loginLogMapper.delete(new QueryWrapper<>());
    }
}
