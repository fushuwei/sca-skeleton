package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackagePageRequest;
import io.github.fushuwei.scaskeleton.system.entity.SysTenantPackage;
import org.apache.ibatis.annotations.Param;

/**
 * 租户套餐 Mapper。
 *
 * @author Fu Wei
 */
public interface SysTenantPackageMapper extends BaseMapper<SysTenantPackage> {

    /**
     * 分页查询套餐，关联子查询一次性查出权限数量。
     *
     * @param page 分页对象
     * @param req  分页查询请求参数（含关键词、状态、排序等）
     * @return 分页结果（每条记录含 permissionCount）
     */
    IPage<SysTenantPackage> selectPackagePage(IPage<SysTenantPackage> page,
                                              @Param("req") TenantPackagePageRequest req);
}
