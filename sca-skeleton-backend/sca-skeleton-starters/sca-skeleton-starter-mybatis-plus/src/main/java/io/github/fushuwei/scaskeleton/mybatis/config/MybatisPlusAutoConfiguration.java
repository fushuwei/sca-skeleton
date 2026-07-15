package io.github.fushuwei.scaskeleton.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.mybatis.handler.MybatisPlusMetaObjectHandler;
import io.github.fushuwei.scaskeleton.mybatis.incrementer.UuidV7IdentifierGenerator;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis-Plus 自动配置类
 *
 * @author Fu Wei
 */
@AutoConfiguration
public class MybatisPlusAutoConfiguration {

    /**
     * 注册 MyBatis-Plus 拦截器链（顺序敏感，分页插件须最后注册）
     *
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 防全表更新/删除，生产安全兜底，避免误操作导致数据全量覆盖
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        // 乐观锁，配合实体类中的 @Version 字段控制并发更新
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 分页插件，必须最后注册，DbType.MYSQL 针对 MySQL 方言生成 LIMIT 分页 SQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }

    /**
     * 自定义 MyBatis-Plus 主键生成器
     *
     * @return UUID v7 主键生成器
     */
    @Bean
    @ConditionalOnMissingBean
    public IdentifierGenerator identifierGenerator() {
        return new UuidV7IdentifierGenerator();
    }

    /**
     * MyBatis-Plus 元对象字段自动填充处理器
     *
     * @return MybatisPlusMetaObjectHandler 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusMetaObjectHandler mybatisPlusMetaObjectHandler(@Autowired(required = false) @Nullable CurrentUserProvider currentUserProvider) {
        return new MybatisPlusMetaObjectHandler(currentUserProvider);
    }
}
