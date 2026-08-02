package io.github.fushuwei.scaskeleton.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.datasource.entity.Driver;
import org.apache.ibatis.annotations.Mapper;

/**
 * 驱动管理 Mapper
 *
 * @author Fu Wei
 */
@Mapper
public interface DriverMapper extends BaseMapper<Driver> {
}
