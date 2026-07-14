package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
     * @param page           分页对象
     * @param keyword        关键词（可选，模糊匹配名称或编码）
     * @param status         状态（可选）
     * @param orderBy        排序字段（可选，白名单校验）
     * @param orderDirection 排序方向 ASC/DESC（可选）
     * @return 分页结果（每条记录含 permissionCount）
     */
    IPage<SysTenantPackage> selectPackagePage(IPage<SysTenantPackage> page,
                                              @Param("keyword") String keyword,
                                              @Param("status") String status,
                                              @Param("orderBy") String orderBy,
                                              @Param("orderDirection") String orderDirection);
}
