package io.github.fushuwei.scaskeleton.mybatis.incrementer;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;

/**
 * 基于 UUID v7 的主键生成器，替代 MyBatis-Plus 默认的随机 UUID
 *
 * @author Fu Wei
 */
public class UuidV7IdentifierGenerator implements IdentifierGenerator {

    @Override
    public Number nextId(Object entity) {
        throw new UnsupportedOperationException("本生成器仅支持 ASSIGN_UUID，不支持 ASSIGN_ID");
    }

    @Override
    public String nextUUID(Object entity) {
        return UuidUtils.nextSimpleStr();
    }
}
