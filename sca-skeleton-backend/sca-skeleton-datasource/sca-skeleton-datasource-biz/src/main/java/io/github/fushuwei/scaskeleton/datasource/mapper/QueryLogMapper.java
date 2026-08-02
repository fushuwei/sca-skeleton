package io.github.fushuwei.scaskeleton.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.datasource.entity.QueryLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * SQL 查询历史 Mapper
 *
 * @author Fu Wei
 */
@Mapper
public interface QueryLogMapper extends BaseMapper<QueryLog> {
}
