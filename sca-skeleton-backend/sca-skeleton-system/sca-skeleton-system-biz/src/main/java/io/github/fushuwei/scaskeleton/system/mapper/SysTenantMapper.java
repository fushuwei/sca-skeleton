package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantPageRequest;
import io.github.fushuwei.scaskeleton.system.entity.SysTenant;
import org.apache.ibatis.annotations.Param;

/**
 * 租户 Mapper。
 *
 * @author Fu Wei
 */
public interface SysTenantMapper extends BaseMapper<SysTenant> {

    /**
     * 分页查询租户，左关联套餐表查出套餐名称。
     */
    IPage<SysTenant> selectTenantPage(IPage<SysTenant> page, @Param("req") TenantPageRequest req);
}
