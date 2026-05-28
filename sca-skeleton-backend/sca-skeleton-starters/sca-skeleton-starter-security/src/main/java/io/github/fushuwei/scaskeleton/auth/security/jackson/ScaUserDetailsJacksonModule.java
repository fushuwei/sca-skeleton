package io.github.fushuwei.scaskeleton.auth.security.jackson;

import io.github.fushuwei.scaskeleton.auth.security.ScaUserDetails;
import org.springframework.security.jackson.SecurityJacksonModule;
import tools.jackson.core.Version;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * 注册 {@link ScaUserDetails} 的 OAuth2 Redis 序列化/反序列化支持。
 */
public final class ScaUserDetailsJacksonModule extends SecurityJacksonModule {

    public ScaUserDetailsJacksonModule() {
        super(ScaUserDetailsJacksonModule.class.getName(), new Version(1, 0, 0, null, null, null));
    }

    @Override
    public void configurePolymorphicTypeValidator(BasicPolymorphicTypeValidator.Builder builder) {
        builder.allowIfSubType(ScaUserDetails.class);
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixIn(ScaUserDetails.class, ScaUserDetailsMixin.class);
    }
}
