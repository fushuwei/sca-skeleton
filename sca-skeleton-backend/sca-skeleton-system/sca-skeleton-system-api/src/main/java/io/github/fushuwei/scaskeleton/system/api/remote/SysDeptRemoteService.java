package io.github.fushuwei.scaskeleton.system.api.remote;

import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 部门管理远程调用服务
 *
 * @author Fu Wei
 */
@HttpExchange("/dept")
public interface SysDeptRemoteService {

    /**
     * 查询当前租户下部门列表
     *
     * @return 部门列表
     */
    @GetExchange("/list")
    List<DeptResponse> list();

    /**
     * 根据 ID 查询部门信息
     *
     * @param id 部门 ID
     * @return 部门信息
     */
    @GetExchange("/{id}")
    DeptResponse getById(@PathVariable String id);
}
