package io.github.fushuwei.scaskeleton.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.datasource.entity.DsQueryLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * SQL 查询历史 Mapper
 *
 * @author Fu Wei
 */
@Mapper
public interface DsQueryLogMapper extends BaseMapper<DsQueryLog> {
}
