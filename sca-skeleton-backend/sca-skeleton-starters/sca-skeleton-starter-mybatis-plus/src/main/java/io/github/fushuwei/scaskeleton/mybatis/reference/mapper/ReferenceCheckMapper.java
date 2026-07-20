package io.github.fushuwei.scaskeleton.mybatis.reference.mapper;

import io.github.fushuwei.scaskeleton.mybatis.reference.ReferenceCheckBatchResult;
import io.github.fushuwei.scaskeleton.mybatis.reference.ReferenceCheckResult;
import io.github.fushuwei.scaskeleton.mybatis.reference.ReferenceParam;
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
     * @param id         被检查的实体 ID
     * @return 每条引用关系的检查结果
     */
    List<ReferenceCheckResult> checkReferences(@Param("references") List<ReferenceParam> references, @Param("id") String id);

    /**
     * 批量删除前检查所有引用关系
     *
     * @param references 引用关系参数列表
     * @param ids        被检查的实体 ID 列表
     * @return 每条 (entityId, reference) 组合的检查结果
     */
    List<ReferenceCheckBatchResult> checkReferencesBatch(@Param("references") List<ReferenceParam> references, @Param("ids") List<String> ids);
}
