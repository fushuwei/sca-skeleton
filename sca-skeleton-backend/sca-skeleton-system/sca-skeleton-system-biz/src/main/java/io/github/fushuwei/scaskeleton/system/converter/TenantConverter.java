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

    /** SysTenant → TenantResponse */
    TenantResponse toTenantResponse(SysTenant tenant);
}
