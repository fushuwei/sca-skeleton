package io.github.fushuwei.scaskeleton.core.validation;

import jakarta.validation.groups.Default;

/**
 * 参数校验分组定义。
 * <p>
 * 用于区分新增（{@link Create}）和更新（{@link Update}）场景下的不同校验规则。
 * 配合 {@code @Validated(ValidGroup.Create.class)} 使用。
 * <p>
 * 各分组均继承 {@link Default}，确保未显式指定分组的约束注解在所有场景下均生效。
 *
 * @author Fu Wei
 */
public interface ValidGroup {

    /**
     * 新增操作分组：通常要求 ID 为空，业务必填字段全量校验。
     */
    interface Create extends Default {
    }

    /**
     * 更新操作分组：通常要求 ID 非空，其余字段按需校验。
     */
    interface Update extends Default {
    }

    /**
     * 查询操作分组：适用于复杂查询条件的参数校验。
     */
    interface Query extends Default {
    }

    /**
     * 删除操作分组：通常仅要求 ID 非空。
     */
    interface Delete extends Default {
    }
}
