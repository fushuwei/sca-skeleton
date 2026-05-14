package io.github.fushuwei.scaskeleton.core.jackson;

import tools.jackson.core.json.PackageVersion;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * 统一设置 java.lang 类型序列化/反序列化格式
 *
 * @author Fu Wei
 */
public final class JavaLangModule extends SimpleModule {

    /**
     * 构造函数
     */
    public JavaLangModule() {
        super(PackageVersion.VERSION);

        // Long 类型的序列化，将 Long 类型序列化为字符串，防止精度丢失问题
        this.addSerializer(Long.class, ToStringSerializer.instance);
        this.addSerializer(Long.TYPE, ToStringSerializer.instance);
    }
}
