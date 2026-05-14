package io.github.fushuwei.scaskeleton.core.jackson;

import tools.jackson.core.json.PackageVersion;
import tools.jackson.databind.ext.javatime.deser.DurationDeserializer;
import tools.jackson.databind.ext.javatime.deser.InstantDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.DurationSerializer;
import tools.jackson.databind.ext.javatime.ser.InstantSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一设置 java.time 类型序列化/反序列化格式
 *
 * @author Fu Wei
 */
public final class JavaTimeModule extends SimpleModule {

    /**
     * 时间字符串格式
     */
    private static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 日期字符串格式
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 日期时间字符串格式
     */
    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 构造函数
     */
    public JavaTimeModule() {
        super(PackageVersion.VERSION);

        // 序列化
        this.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));
        this.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        this.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));
        this.addSerializer(Instant.class, InstantSerializer.INSTANCE);
        this.addSerializer(Duration.class, DurationSerializer.INSTANCE);

        // 反序列化
        this.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));
        this.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        this.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));
        this.addDeserializer(Instant.class, InstantDeserializer.INSTANT);
        this.addDeserializer(Duration.class, DurationDeserializer.INSTANCE);
    }
}
