package io.github.fushuwei.sca.starter.mybatis.config;

import io.github.fushuwei.sca.starter.core.user.CurrentUserProvider;
import io.github.fushuwei.sca.starter.mybatis.handler.MybatisPlusMetaObjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;

/**
 * MyBatis-Plus Starter 自动配置入口。
 * <p>
 * 统一注册：插件链（分页/乐观锁/防全表更新）、审计字段自动填充处理器。
 *
 * @author Fu Wei
 */
@AutoConfiguration
@Import(MybatisPlusConfig.class)
public class MybatisPlusAutoConfiguration {

    /**
     * 注册审计字段自动填充处理器，注入可选的 CurrentUserProvider。
     *
     * @param currentUserProvider 当前用户信息提供者（可选，由 Security Starter 提供）
     * @return MybatisPlusMetaObjectHandler 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusMetaObjectHandler mybatisPlusMetaObjectHandler(
            @Autowired(required = false) @Nullable CurrentUserProvider currentUserProvider) {
        return new MybatisPlusMetaObjectHandler(currentUserProvider);
    }
}
