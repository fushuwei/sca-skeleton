package io.github.fushuwei.scaskeleton.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置。
 * <p>
 * 注册以下插件（注意：插件链顺序会影响行为，分页插件必须最后注册）：
 * <ol>
 *   <li>{@link BlockAttackInnerInterceptor}：防全表更新/删除，拦截无 WHERE 条件的 UPDATE/DELETE</li>
 *   <li>{@link OptimisticLockerInnerInterceptor}：乐观锁，配合 {@code @Version} 注解使用</li>
 *   <li>{@link PaginationInnerInterceptor}：分页插件，针对 MySQL 进行 SQL 优化</li>
 * </ol>
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器链。使用 ConditionalOnMissingBean 允许业务模块覆盖。
     *
     * @return MybatisPlusInterceptor 插件链实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 防全表更新/删除，生产安全兜底，避免误操作导致数据全量覆盖
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        // 乐观锁，在并发更新场景下通过版本号控制并发，需实体字段添加 @Version
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 分页插件，必须放在最后注册；DbType.MYSQL 针对 MySQL 方言生成 LIMIT 分页 SQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
