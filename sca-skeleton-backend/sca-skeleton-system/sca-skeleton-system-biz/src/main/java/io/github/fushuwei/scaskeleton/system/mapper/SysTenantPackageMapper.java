package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackagePageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysTenantPackage;
import org.apache.ibatis.annotations.Param;

/**
 * 租户套餐管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysTenantPackageMapper extends BaseMapper<SysTenantPackage> {

    /**
     * 分页查询租户套餐列表
     *
     * @param page    分页对象
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<TenantPackageResponse> selectPackagePage(IPage<TenantPackageResponse> page, @Param("request") TenantPackagePageRequest request);
}
