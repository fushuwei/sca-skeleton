package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.tenant.TenantResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysTenant;
import org.mapstruct.Mapper;

/**
 * 租户对象转换器（MapStruct）
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface TenantConverter {

    /**
     * 租户实体 → 租户响应
     *
     * @param tenant 租户实体
     * @return 租户响应对象
     */
    TenantResponse toTenantResponse(SysTenant tenant);
}
