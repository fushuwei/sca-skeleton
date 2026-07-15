package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.log.entity.SysLoginLog;
import io.github.fushuwei.scaskeleton.log.mapper.SysLoginLogMapper;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
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
 * 登录日志管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLoginLogServiceImpl implements SysLoginLogService {

    private final SysLoginLogMapper loginLogMapper;

    private final LoginLogConverter loginLogConverter;

    /**
     * 分页查询登录日志
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<LoginLogResponse> pageLogs(LoginLogPageRequest request) {
        // 构造分页对象
        Page<SysLoginLog> page = new Page<>(request.getPageNum(), request.getPageSize());
        // 查询分页数据，并将结果转换为响应对象
        IPage<SysLoginLog> entityPage = loginLogMapper.selectLogPage(page,
            request.getKeyword(),
            request.getIsSuccess(),
            request.getStartTime(),
            request.getEndTime(),
            request.safeSortField(),
            request.safeSortOrder());
        return entityPage.convert(loginLogConverter::toLoginLogResponse);
    }

    /**
     * 根据 ID 查询登录日志详情
     *
     * @param id 登录日志 ID
     * @return 登录日志详情
     */
    @Override
    public LoginLogResponse getLogById(String id) {
        // 按主键查询日志
        SysLoginLog logEntity = loginLogMapper.selectLogById(id);
        if (logEntity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日志不存在");
        }
        // 转换为响应对象
        return loginLogConverter.toLoginLogResponse(logEntity);
    }

    /**
     * 批量删除登录日志
     *
     * @param ids 登录日志 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteLogs(List<String> ids) {
        // 仅超级管理员可批量删除登录日志
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可批量删除登录日志");
        }
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        loginLogMapper.deleteBatchIds(ids);
    }

    /**
     * 清空全部登录日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllLogs() {
        // 仅超级管理员可清空登录日志
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可清空登录日志");
        }
        loginLogMapper.delete(null);
    }
}
