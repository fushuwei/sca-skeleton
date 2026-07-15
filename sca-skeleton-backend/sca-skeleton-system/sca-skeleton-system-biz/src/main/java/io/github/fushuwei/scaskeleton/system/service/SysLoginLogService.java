package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.loginlog.LoginLogPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.loginlog.LoginLogResponse;

import java.util.List;

/**
 * 登录日志管理 Service
 *
 * @author Fu Wei
 */
public interface SysLoginLogService {

    /**
     * 分页查询登录日志
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<LoginLogResponse> pageLogs(LoginLogPageRequest request);

    /**
     * 根据 ID 查询登录日志详情
     *
     * @param id 登录日志 ID
     * @return 登录日志详情
     */
    LoginLogResponse getLogById(String id);

    /**
     * 批量删除登录日志
     *
     * @param ids 登录日志 ID 列表
     */
    void batchDeleteLogs(List<String> ids);

    /**
     * 清空全部登录日志
     */
    void clearAllLogs();
}
