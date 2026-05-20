package io.github.fushuwei.scaskeleton.core.trace;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import org.slf4j.MDC;

/**
 * 链路追踪上下文。
 * <p>
 * 维护当前请求的 TraceId，同步写入 SLF4J MDC，使日志能自动携带链路标识。
 * HTTP 请求头 Key 见 {@link GlobalConstants#HEADER_TRACE_ID}。
 * Web 过滤器（{@code TraceIdFilter}）在请求入口注入，Feign 拦截器在跨服务调用时传递，
 * 异步任务场景需手动传递（通过 {@link #copy()} 获取当前值，在新线程中调用 {@link #set(String)}）。
 *
 * @author Fu Wei
 */
public final class TraceContext {

    private TraceContext() {
    }

    /**
     * 将 traceId 写入 MDC，使当前线程后续的所有日志自动携带该标识。
     *
     * @param traceId 链路追踪 ID，通常为 32 位无连字符 UUID
     */
    public static void set(String traceId) {
        MDC.put(GlobalConstants.MDC_TRACE_ID, traceId);
    }

    /**
     * 从 MDC 读取当前 traceId。
     *
     * @return 当前线程的 traceId，未设置时返回 {@code null}
     */
    public static String get() {
        return MDC.get(GlobalConstants.MDC_TRACE_ID);
    }

    /**
     * 清理 MDC 中的 traceId，防止线程池场景下出现上下文污染。
     * 应在请求处理结束后（filter finally 块）调用。
     */
    public static void clear() {
        MDC.remove(GlobalConstants.MDC_TRACE_ID);
    }

    /**
     * 复制当前 traceId 用于异步传递。
     * 在提交异步任务前调用，将返回值在新线程的 {@link #set(String)} 中设置。
     *
     * @return 当前 traceId 快照，可能为 {@code null}
     */
    public static String copy() {
        return MDC.get(GlobalConstants.MDC_TRACE_ID);
    }
}
