package io.github.fushuwei.scaskeleton.mybatis.reference.mapper;

import io.github.fushuwei.scaskeleton.mybatis.reference.dto.ReferenceCheckBatchResult;
import io.github.fushuwei.scaskeleton.mybatis.reference.dto.ReferenceCheckResult;
import io.github.fushuwei.scaskeleton.mybatis.reference.dto.ReferenceParam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 引用检查 Mapper
 *
 * @author Fu Wei
 */
public interface ReferenceCheckMapper {

    /**
     * 单个删除前检查所有引用关系
     *
     * @param references 引用关系参数列表
     * @param id         被删除实体的 ID
     * @return 单条引用检查结果
     */
    List<ReferenceCheckResult> checkReferences(@Param("references") List<ReferenceParam> references, @Param("id") String id);

    /**
     * 批量删除前检查所有引用关系
     *
     * @param references      引用关系参数列表
     * @param ids             被删除实体的 ID 列表
     * @param entityTableName 被删除实体的表名
     * @param displayColumn   展示字段名
     * @return 批量删除时的引用检查结果
     */
    List<ReferenceCheckBatchResult> checkReferencesBatch(@Param("references") List<ReferenceParam> references,
                                                         @Param("ids") List<String> ids,
                                                         @Param("entityTableName") String entityTableName,
                                                         @Param("displayColumn") String displayColumn);
}
