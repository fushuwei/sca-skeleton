package io.github.fushuwei.scaskeleton.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverPageRequest;
import io.github.fushuwei.scaskeleton.datasource.entity.Driver;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 驱动管理 Mapper
 *
 * @author Fu Wei
 */
@Mapper
public interface DriverMapper extends BaseMapper<Driver> {

    /**
     * 分页查询驱动列表（支持动态排序，排序字段经白名单校验）。
     *
     * @param page    分页对象（由 MyBatis-Plus 分页插件自动填充 total 等字段）
     * @param request 查询请求（含筛选条件与排序参数）
     * @return 分页结果
     */
    IPage<Driver> pageDrivers(IPage<Driver> page, @Param("request") DriverPageRequest request);
}
