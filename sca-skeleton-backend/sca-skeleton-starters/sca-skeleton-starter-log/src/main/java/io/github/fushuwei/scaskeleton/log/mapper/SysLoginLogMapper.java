package io.github.fushuwei.scaskeleton.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.log.entity.SysLoginLog;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 登录日志 Mapper
 *
 * @author Fu Wei
 */
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    /**
     * 分页查询登录日志
     *
     * @param page           分页对象
     * @param keyword        搜索关键字（模糊匹配租户名称、登录用户、真实姓名、客户端 IP）
     * @param isSuccess      是否成功筛选：1-成功，0-失败
     * @param startTime      查询开始时间
     * @param endTime        查询结束时间
     * @param sortField      排序字段（白名单校验）
     * @param sortOrder      排序方向 ASC/DESC
     * @return 分页结果
     */
    IPage<SysLoginLog> selectLogPage(IPage<SysLoginLog> page,
                                     @Param("keyword") String keyword,
                                     @Param("isSuccess") Integer isSuccess,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     @Param("sortField") String sortField,
                                     @Param("sortOrder") String sortOrder);

    /**
     * 按 ID 查询登录日志详情
     *
     * @param id 登录日志 ID
     * @return 登录日志对象
     */
    SysLoginLog selectLogById(@Param("id") String id);
}
