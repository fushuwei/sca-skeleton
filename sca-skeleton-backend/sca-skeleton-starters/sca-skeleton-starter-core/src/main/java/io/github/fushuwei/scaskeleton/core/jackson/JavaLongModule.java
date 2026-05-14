package io.github.fushuwei.scaskeleton.core.jackson;

import tools.jackson.core.json.PackageVersion;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * 统一设置 Long 类型序列化格式
 * 默认将 Long 类型序列化为字符串，防止精度丢失问题
 *
 * @author Fu Wei
 */
public final class JavaLongModule extends SimpleModule {

    /**
     * 构造函数
     */
    public JavaLongModule() {
        super(PackageVersion.VERSION);

        // 序列化
        this.addSerializer(Long.class, ToStringSerializer.instance);
        this.addSerializer(Long.TYPE, ToStringSerializer.instance);
    }
}
