package io.github.fushuwei.sca.starter.mybatis.base;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公共基础实体。
 * <p>
 * 所有数据库实体类继承此类，统一获得以下能力：
 * <ul>
 *   <li>主键：32 位小写无连字符 UUID，由 {@link MetaObjectHandler} 自动填充</li>
 *   <li>创建时间 / 更新时间：由 {@link MetaObjectHandler} 自动填充与更新</li>
 *   <li>创建人 ID / 更新人 ID：由 {@link MetaObjectHandler} 从 {@code CurrentUserProvider} 读取</li>
 *   <li>逻辑删除：{@code isDeleted=0} 未删除，{@code isDeleted=1} 已删除，查询时自动过滤</li>
 * </ul>
 * <p>
 * 字段命名采用驼峰，开启 {@code map-underscore-to-camel-case} 后自动与数据库下划线字段对应：
 * {@code isDeleted} ↔ {@code is_deleted}，{@code createTime} ↔ {@code create_time} 等。
 *
 * @author Fu Wei
 */
@Getter
@Setter
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键：32 位小写无连字符 UUID，INSERT 时由 MetaObjectHandler 自动填充，禁止使用自增 ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 创建时间，INSERT 时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最后更新时间，INSERT 和 UPDATE 时均自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 ID，INSERT 时从 CurrentUserProvider 自动读取 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 最后更新人 ID，INSERT 和 UPDATE 时均自动读取 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 逻辑删除标识，映射数据库列 {@code is_deleted}（下划线转驼峰）：0-未删除，1-已删除。
     * MyBatis-Plus 查询时自动附加 WHERE is_deleted=0，无需在每条 SQL 中手写。
     */
    @TableLogic
    private Integer isDeleted;
}
