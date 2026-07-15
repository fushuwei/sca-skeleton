package io.github.fushuwei.scaskeleton.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 元对象字段自动填充处理器
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    private final CurrentUserProvider currentUserProvider;

    /**
     * INSERT 操作时填充逻辑（用于插入时对公共字段的填充）
     *
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 填充当前用户作为创建人与最后更新人
        String userId = getCurrentUserId();
        this.strictInsertFill(metaObject, "createBy", String.class, userId);
        this.strictInsertFill(metaObject, "updateBy", String.class, userId);

        // 填充当前时间作为创建时间与更新时间
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    /**
     * UPDATE 操作时填充逻辑（用于更新时对公共字段的填充）
     *
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 填充当前用户作为最后更新人
        this.strictUpdateFill(metaObject, "updateBy", String.class, getCurrentUserId());

        // 填充当前时间作为更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 获取当前用户 ID
     *
     * @return 当前用户 ID
     */
    private String getCurrentUserId() {
        // 如果未登录，则返回 null
        if (currentUserProvider == null) {
            return null;
        }
        try {
            return currentUserProvider.getUserId();
        } catch (Exception e) {
            log.warn("[MyBatisPlus 元对象字段自动填充处理器] 无法获取当前用户 ID", e);
            return null;
        }
    }
}
