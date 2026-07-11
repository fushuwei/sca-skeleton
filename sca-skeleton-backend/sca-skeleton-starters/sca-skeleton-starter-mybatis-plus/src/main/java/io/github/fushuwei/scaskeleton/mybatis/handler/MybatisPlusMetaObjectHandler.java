package io.github.fushuwei.scaskeleton.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 审计字段自动填充处理器。
 * <p>
 * 在 INSERT / UPDATE 时自动填充 {@code BaseEntity} 中的以下字段：
 * <ul>
 *   <li>{@code createTime}：INSERT 时填充当前时间</li>
 *   <li>{@code updateTime}：INSERT 和 UPDATE 时均填充当前时间</li>
 *   <li>{@code createBy}：INSERT 时从 {@link CurrentUserProvider} 读取当前用户 ID</li>
 *   <li>{@code updateBy}：INSERT 和 UPDATE 时均从 {@link CurrentUserProvider} 读取当前用户 ID</li>
 * </ul>
 * <p>
 * 主键 {@code id} 由 {@link io.github.fushuwei.scaskeleton.mybatis.incrementer.UuidV7IdentifierGenerator}
 * 通过 {@code @TableId(type = IdType.ASSIGN_UUID)} 机制生成，不在此处理。
 *
 * @author Fu Wei
 */
@Slf4j
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    // 可选注入：未引入 Security Starter 时为 null，审计人字段将留空
    @Nullable
    private final CurrentUserProvider currentUserProvider;

    public MybatisPlusMetaObjectHandler(@Nullable CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * INSERT 时触发，填充 createTime、updateTime、createBy、updateBy。
     *
     * @param metaObject MyBatis 元对象，用于读写实体字段
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 记录当前时间作为创建与更新时间
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        // 记录当前操作人 ID（依赖 Security Starter 提供的实现）
        String userId = resolveCurrentUserId();
        this.strictInsertFill(metaObject, "createBy", String.class, userId);
        this.strictInsertFill(metaObject, "updateBy", String.class, userId);
    }

    /**
     * UPDATE 时触发，填充 updateTime、updateBy。
     *
     * @param metaObject MyBatis 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时间强制覆盖为当前时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // 更新人 ID 强制覆盖为当前操作人
        this.strictUpdateFill(metaObject, "updateBy", String.class, resolveCurrentUserId());
    }

    /**
     * 安全获取当前用户 ID，未注入 Provider 或用户未认证时返回 null。
     *
     * @return 当前用户 ID 字符串，可能为 null
     */
    private String resolveCurrentUserId() {
        if (currentUserProvider == null) {
            // 未引入 Security Starter 时跳过用户 ID 填充
            return null;
        }
        try {
            return currentUserProvider.getUserId();
        } catch (Exception e) {
            // 审计字段填充失败不应中断业务操作，仅打印警告
            log.warn("[MyBatisPlus] failed to get current user id for audit fill", e);
            return null;
        }
    }
}
