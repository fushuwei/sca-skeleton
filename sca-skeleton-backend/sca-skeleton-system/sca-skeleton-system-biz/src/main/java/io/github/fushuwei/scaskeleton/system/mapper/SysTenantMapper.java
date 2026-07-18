package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.tenant.TenantResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysTenant;
import org.apache.ibatis.annotations.Param;

/**
 * 租户管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysTenantMapper extends BaseMapper<SysTenant> {

    /**
     * 分页查询租户列表
     *
     * @param page    分页对象
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<TenantResponse> selectTenantPage(IPage<TenantResponse> page, @Param("request") TenantPageRequest request);
}
