package io.github.fushuwei.scaskeleton.web.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * 全局 java.time 类型请求参数格式化自动配置。
 * <p>
 * Spring MVC GET 请求的查询参数绑定使用的是 {@link FormatterRegistry} 中的
 * {@link Formatter}，而非 Jackson 的 {@link tools.jackson.databind.ObjectMapper}。
 * Jackson 配置（如 {@code JavaTimeModule}）仅对 <em>JSON 请求体</em>（{@code @RequestBody}）的反序列化生效，
 * 对 URL 查询参数（{@code ?startTime=xxx}）不生效。
 * <p>
 * 本配置为 {@link LocalDateTime}、{@link LocalDate}、{@link LocalTime} 注册全局默认格式化器，
 * 与 {@code JavaTimeModule} 中的格式保持一致，同时兼容 ISO-8601 标准格式。
 *
 * @author Fu Wei
 */
@AutoConfiguration
public class DateTimeFormatterAutoConfiguration implements WebMvcConfigurer {

    /**
     * 项目约定：日期时间格式
     */
    static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 项目约定：日期格式
     */
    static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 项目约定：时间格式
     */
    static final String TIME_PATTERN = "HH:mm:ss";

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatterForFieldType(LocalDateTime.class, new MultiFormatLocalDateTimeFormatter());
        registry.addFormatterForFieldType(LocalDate.class, new MultiFormatLocalDateFormatter());
        registry.addFormatterForFieldType(LocalTime.class, new MultiFormatLocalTimeFormatter());
    }

    /**
     * LocalDateTime 格式化器，优先项目约定格式，回退 ISO-8601。
     */
    static class MultiFormatLocalDateTimeFormatter implements Formatter<LocalDateTime> {

        private static final DateTimeFormatter[] FORMATTERS = {
                DateTimeFormatter.ofPattern(DATETIME_PATTERN),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };

        @Override
        public LocalDateTime parse(String text, Locale locale) throws ParseException {
            if (text == null || text.isBlank()) {
                return null;
            }
            for (DateTimeFormatter fmt : FORMATTERS) {
                try {
                    return LocalDateTime.parse(text, fmt);
                } catch (DateTimeParseException ignored) {
                    // 尝试下一个格式
                }
            }
            throw new ParseException("无法解析 '" + text + "' 为 LocalDateTime（支持格式：" + DATETIME_PATTERN + " / ISO-8601）", 0);
        }

        @Override
        public String print(LocalDateTime object, Locale locale) {
            return FORMATTERS[0].format(object);
        }
    }

    /**
     * LocalDate 格式化器，优先项目约定格式，回退 ISO-8601。
     */
    static class MultiFormatLocalDateFormatter implements Formatter<LocalDate> {

        private static final DateTimeFormatter[] FORMATTERS = {
                DateTimeFormatter.ofPattern(DATE_PATTERN),
                DateTimeFormatter.ISO_LOCAL_DATE
        };

        @Override
        public LocalDate parse(String text, Locale locale) throws ParseException {
            if (text == null || text.isBlank()) {
                return null;
            }
            for (DateTimeFormatter fmt : FORMATTERS) {
                try {
                    return LocalDate.parse(text, fmt);
                } catch (DateTimeParseException ignored) {
                    // 尝试下一个格式
                }
            }
            throw new ParseException("无法解析 '" + text + "' 为 LocalDate（支持格式：" + DATE_PATTERN + " / ISO-8601）", 0);
        }

        @Override
        public String print(LocalDate object, Locale locale) {
            return FORMATTERS[0].format(object);
        }
    }

    /**
     * LocalTime 格式化器，项目约定格式。
     */
    static class MultiFormatLocalTimeFormatter implements Formatter<LocalTime> {

        private static final DateTimeFormatter[] FORMATTERS = {
                DateTimeFormatter.ofPattern(TIME_PATTERN),
                DateTimeFormatter.ISO_LOCAL_TIME
        };

        @Override
        public LocalTime parse(String text, Locale locale) throws ParseException {
            if (text == null || text.isBlank()) {
                return null;
            }
            for (DateTimeFormatter fmt : FORMATTERS) {
                try {
                    return LocalTime.parse(text, fmt);
                } catch (DateTimeParseException ignored) {
                    // 尝试下一个格式
                }
            }
            throw new ParseException("无法解析 '" + text + "' 为 LocalTime（支持格式：" + TIME_PATTERN + " / ISO-8601）", 0);
        }

        @Override
        public String print(LocalTime object, Locale locale) {
            return FORMATTERS[0].format(object);
        }
    }
}
