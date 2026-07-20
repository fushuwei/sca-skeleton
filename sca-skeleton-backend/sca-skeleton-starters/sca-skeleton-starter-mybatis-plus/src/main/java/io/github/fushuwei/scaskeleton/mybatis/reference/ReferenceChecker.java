package io.github.fushuwei.scaskeleton.mybatis.reference;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.ReferencedBy;
import io.github.fushuwei.scaskeleton.mybatis.reference.dto.ReferenceCheckBatchResult;
import io.github.fushuwei.scaskeleton.mybatis.reference.dto.ReferenceCheckResult;
import io.github.fushuwei.scaskeleton.mybatis.reference.dto.ReferenceParam;
import io.github.fushuwei.scaskeleton.mybatis.reference.mapper.ReferenceCheckMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 引用检查器
 *
 * @author Fu Wei
 */
@RequiredArgsConstructor
public class ReferenceChecker {

    private final ReferenceCheckMapper referenceCheckMapper;

    /**
     * 注解解析缓存
     * <p>
     * Key: 实体类，Value: 该实体类上 {@link ReferencedBy} 注解解析出的引用关系参数列表
     * <p>
     * 注解在运行期不变，缓存零风险，同时将注解对象转换为标准 JavaBean，避免 MyBatis 反射访问注解属性时因 JDK 动态代理导致 ReflectionException
     */
    private final Map<Class<?>, List<ReferenceParam>> referenceCache = new ConcurrentHashMap<>();

    /**
     * 表名缓存
     * <p>
     * Key: 实体类，Value: 该实体类对应的数据库表名（来自 {@link TableName} 注解）
     */
    private final Map<Class<?>, String> entityTableNameCache = new ConcurrentHashMap<>();

    /**
     * 展示字段缓存
     * <p>
     * Key: 实体类，Value: 该实体类的展示字段名（来自 {@link ReferencedBy#displayColumn()}），空字符串表示未声明
     */
    private final Map<Class<?>, String> displayColumnCache = new ConcurrentHashMap<>();

    /**
     * 删除前引用校验
     * <p>
     * 实体类未标注 {@link ReferencedBy} 时直接返回，跳过校验
     *
     * @param entityClass 被删除实体的类型
     * @param id          被删除实体的 ID
     */
    public void check(Class<?> entityClass, String id) {
        List<ReferenceParam> references = resolveReferences(entityClass);
        if (references.isEmpty()) {
            return;
        }

        List<ReferenceCheckResult> results = referenceCheckMapper.checkReferences(references, id);
        if (CollectionUtils.isEmpty(results)) {
            return;
        }

        List<String> blockedMessages = results.stream()
            .filter(ReferenceCheckResult::getHasReference)
            .map(ReferenceCheckResult::getMessage)
            .toList();

        if (!blockedMessages.isEmpty()) {
            throw new BusinessException(ResultCode.DATA_REFERENCED, String.join("；", blockedMessages));
        }
    }

    /**
     * 批量删除前引用校验
     * <p>
     * 实体类未标注 {@link ReferencedBy} 时直接返回，跳过校验
     *
     * @param entityClass 被删除实体的类型
     * @param ids         被删除实体的 ID 列表
     */
    public void checkBatch(Class<?> entityClass, List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        List<ReferenceParam> references = resolveReferences(entityClass);
        if (references.isEmpty()) {
            return;
        }

        List<ReferenceCheckBatchResult> results = referenceCheckMapper.checkReferencesBatch(references,
            ids, resolveEntityTableName(entityClass), resolveDisplayColumn(entityClass));
        if (CollectionUtils.isEmpty(results)) {
            return;
        }

        // 按被删除实体的 ID 分组，收集所有被引用的实体及原因
        Map<String, List<ReferenceCheckBatchResult>> blockedMap = results.stream()
            .filter(ReferenceCheckBatchResult::getHasReference)
            .collect(Collectors.groupingBy(ReferenceCheckBatchResult::getId));

        if (blockedMap.isEmpty()) {
            return;
        }

        // 一次性返回所有被引用的 ID 及原因，避免用户反复试
        String message = blockedMap.values().stream()
            .map(list -> {
                // 同一 entityId 的所有行 displayName 相同，取第一行即可
                String displayName = list.getFirst().getDisplayName();
                String reasons = list.stream()
                    .map(ReferenceCheckBatchResult::getMessage)
                    .collect(Collectors.joining("、"));
                return displayName + "：" + reasons;
            })
            .collect(Collectors.joining("；"));

        throw new BusinessException(ResultCode.DATA_REFERENCED, message);
    }

    /**
     * 解析实体类上的 {@link ReferencedBy} 注解，返回引用关系参数列表
     * <p>
     * 结果会被缓存，避免每次删除都调用反射
     *
     * @param entityClass 实体类型
     * @return 引用关系参数列表，未标注注解时返回空列表
     */
    private List<ReferenceParam> resolveReferences(Class<?> entityClass) {
        return referenceCache.computeIfAbsent(entityClass, cls -> {
            ReferencedBy annotation = cls.getAnnotation(ReferencedBy.class);
            if (annotation == null) {
                return List.of();
            }
            // 将注解对象转换为标准 JavaBean，避免 MyBatis 反射访问注解属性失败
            return Arrays.stream(annotation.value())
                .map(ref -> new ReferenceParam(ref.table(), ref.column(), ref.message(), ref.logicalDelete()))
                .toList();
        });
    }

    /**
     * 解析实体类对应的数据库表名
     * <p>
     * 通过反射读取 {@link TableName} 注解的 value 属性
     *
     * @param entityClass 实体类型
     * @return 表名，未标注 {@link TableName} 时返回 null
     */
    private String resolveEntityTableName(Class<?> entityClass) {
        return entityTableNameCache.computeIfAbsent(entityClass, cls -> {
            TableName annotation = cls.getAnnotation(TableName.class);
            return annotation != null ? annotation.value() : null;
        });
    }

    /**
     * 解析实体类的展示字段名
     * <p>
     * 结果会被缓存；{@code displayColumn} 默认 "id"，未标注 {@link ReferencedBy} 时也返回 "id"
     *
     * @param entityClass 实体类型
     * @return 展示字段名，默认 "id"
     */
    private String resolveDisplayColumn(Class<?> entityClass) {
        return displayColumnCache.computeIfAbsent(entityClass, cls -> {
            ReferencedBy annotation = cls.getAnnotation(ReferencedBy.class);
            return annotation != null ? annotation.displayColumn() : "id";
        });
    }
}
