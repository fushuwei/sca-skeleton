package io.github.fushuwei.scaskeleton.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.datasource.entity.Datasource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源 Mapper
 *
 * @author Fu Wei
 */
@Mapper
public interface DatasourceMapper extends BaseMapper<Datasource> {
}
