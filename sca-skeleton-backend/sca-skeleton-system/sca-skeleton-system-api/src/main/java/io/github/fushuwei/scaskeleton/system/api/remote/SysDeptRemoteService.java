package io.github.fushuwei.scaskeleton.system.api.remote;

import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * System 服务的部门管理远程调用契约。
 * <p>
 * 其他微服务通过引入 {@code sca-skeleton-system-api} 依赖，配合 RestClient 注入即可调用。
 * 响应体 {@code Result<T>} 由 {@code RemoteResponseInterceptor} 自动解包。
 *
 * @author Fu Wei
 */
@HttpExchange("/dept")
public interface SysDeptRemoteService {

    /**
     * 查询当前租户下部门列表。
     *
     * @return 部门列表
     */
    @GetExchange("/list")
    List<DeptResponse> list();

    /**
     * 按 ID 查询部门详情。
     *
     * @param id 部门 ID
     * @return 部门详情
     */
    @GetExchange("/{id}")
    DeptResponse getById(@PathVariable("id") String id);
}
