package io.github.fushuwei.scaskeleton.system.api.request.operationlog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志分页查询请求对象。
 *
 * @author Fu Wei
 */
@Data
public class OperationLogPageRequest {

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    /** 每页条数，上限 100 防止全表拉取 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer pageSize = 20;

    /** 操作模块筛选 */
    private String module;

    /** 操作动作筛选 */
    private String action;

    /** 操作人用户名筛选 */
    private String username;

    /** 操作状态筛选：true-成功，false-失败 */
    private Boolean success;

    /** 查询开始时间 */
    private LocalDateTime startTime;

    /** 查询结束时间 */
    private LocalDateTime endTime;
}
