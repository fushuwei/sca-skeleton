package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.loginlog.LoginLogPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.loginlog.LoginLogResponse;

import java.util.List;

/**
 * 登录日志服务接口。
 *
 * @author Fu Wei
 */
public interface SysLoginLogService {

    /** 分页查询登录日志 */
    IPage<LoginLogResponse> pageLogs(LoginLogPageRequest request);

    /** 根据 ID 查询登录日志详情 */
    LoginLogResponse getLogById(String id);

    /** 批量删除登录日志 */
    void batchDeleteLogs(List<String> ids);

    /** 清空全部登录日志 */
    void clearAllLogs();
}
