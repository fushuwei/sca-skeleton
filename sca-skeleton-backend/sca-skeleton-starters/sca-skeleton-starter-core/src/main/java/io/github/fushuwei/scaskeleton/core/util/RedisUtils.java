package io.github.fushuwei.scaskeleton.core.util;

import cn.hutool.extra.spring.SpringUtil;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 操作工具类
 * <p>
 * 提供便捷的静态方法操作 Redis 中的 String、Hash、Set、List、ZSet 等数据结构
 *
 * @author Fu Wei
 */
public final class RedisUtils {

    /**
     * RedisTemplate 实例，启动时由 Injector 或 getRedisTemplate 懒加载初始化
     */
    private static volatile RedisTemplate<String, Object> redisTemplate;

    /**
     * 工具类禁止实例化
     */
    private RedisUtils() {
    }

    /**
     * 内部注入器
     * <p>
     * 监听 ApplicationReadyEvent，在 Spring 容器就绪后将 RedisTemplate
     * 注入到静态字段，避免每次操作都通过 ApplicationContext 查找
     */
    @Component
    @RequiredArgsConstructor
    static class Injector implements ApplicationListener<ApplicationReadyEvent> {

        private final RedisTemplate<String, Object> redisTemplate;

        @Override
        public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
            RedisUtils.redisTemplate = redisTemplate;
        }
    }

    /**
     * 获取 RedisTemplate 实例
     * <p>
     * 优先使用已缓存的静态引用；若未被 Injector 初始化（如 ApplicationReadyEvent 尚未触发），
     * 则通过 SpringUtil 懒加载获取并缓存，后续调用不再查找
     */
    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, Object> getRedisTemplate() {
        if (redisTemplate == null) {
            synchronized (RedisUtils.class) {
                if (redisTemplate == null) {
                    redisTemplate = SpringUtil.getBean(RedisTemplate.class);
                }
            }
        }
        return redisTemplate;
    }

    // ======================== Key 操作 ========================

    /**
     * 设置 Key 的过期时间
     *
     * @param key     Redis Key
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return true 表示设置成功
     */
    public static boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(getRedisTemplate().expire(key, timeout, unit));
    }

    /**
     * 设置 Key 的过期时间（秒）
     *
     * @param key     Redis Key
     * @param seconds 过期秒数
     * @return true 表示设置成功
     */
    public static boolean expire(String key, long seconds) {
        return expire(key, seconds, TimeUnit.SECONDS);
    }

    /**
     * 获取 Key 的剩余过期时间
     *
     * @param key  Redis Key
     * @param unit 时间单位
     * @return 剩余时间，Key 不存在或永不过期返回 -2
     */
    public static long getExpire(String key, TimeUnit unit) {
        Long expire = getRedisTemplate().getExpire(key, unit);
        return expire != null ? expire : -2;
    }

    /**
     * 获取 Key 的剩余过期时间（秒）
     *
     * @param key Redis Key
     * @return 剩余秒数，Key 不存在或永不过期返回 -2
     */
    public static long getExpire(String key) {
        return getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断 Key 是否存在
     *
     * @param key Redis Key
     * @return true 表示存在
     */
    public static boolean hasKey(String key) {
        return Boolean.TRUE.equals(getRedisTemplate().hasKey(key));
    }

    /**
     * 删除单个 Key
     *
     * @param key Redis Key
     * @return true 表示删除成功
     */
    public static boolean delete(String key) {
        return Boolean.TRUE.equals(getRedisTemplate().delete(key));
    }

    /**
     * 批量删除 Key
     *
     * @param keys Key 集合
     * @return 实际删除的 Key 数量
     */
    public static long delete(Collection<String> keys) {
        Long count = getRedisTemplate().delete(keys);
        return count != null ? count : 0;
    }

    /**
     * 删除一个或多个 Key
     *
     * @param keys 可变参数 Key 列表
     */
    public static void delete(String... keys) {
        if (keys != null) {
            RedisTemplate<String, Object> t = getRedisTemplate();
            for (String key : keys) {
                t.delete(key);
            }
        }
    }

    /**
     * 使用 KEYS 命令查找匹配的 Key
     * <p>
     * 注意：KEYS 命令会阻塞 Redis 服务器，生产环境请使用 {@link #scan(String)}
     *
     * @param pattern Key 匹配模式，支持通配符 * ? []
     * @return 匹配的 Key 集合
     */
    public static Set<String> keys(String pattern) {
        return getRedisTemplate().keys(pattern);
    }

    /**
     * 使用 SCAN 命令查找匹配的 Key（无数量限制）
     *
     * @param pattern Key 匹配模式
     * @return 匹配的 Key 列表
     */
    public static List<String> scan(String pattern) {
        return scan(pattern, -1);
    }

    /**
     * 使用 SCAN 命令查找匹配的 Key
     *
     * @param pattern Key 匹配模式
     * @param count   每次 SCAN 的约数数量（-1 表示不限制）
     * @return 匹配的 Key 列表
     */
    public static List<String> scan(String pattern, int count) {
        RedisConnectionFactory factory = getRedisTemplate().getConnectionFactory();
        Objects.requireNonNull(factory, "RedisConnectionFactory must not be null");

        // 借用底层连接执行 SCAN，避免 KEYS 命令阻塞 Redis
        RedisConnection connection = factory.getConnection();
        try {
            // count > 0 时指定每次迭代约数，-1 表示不限制
            ScanOptions options = count > 0
                ? ScanOptions.scanOptions().match(pattern).count(count).build()
                : ScanOptions.scanOptions().match(pattern).build();
            Cursor<byte[]> cursor = connection.keyCommands().scan(options);
            List<String> result = new ArrayList<>();
            while (cursor.hasNext()) {
                result.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
            return result;
        } finally {
            // 归还连接到连接池，防止连接泄漏
            RedisConnectionUtils.releaseConnection(connection, factory);
        }
    }

    /**
     * 分页查询匹配的 Key（基于 SCAN 命令模拟分页）
     *
     * @param pattern Key 匹配模式
     * @param page    页码（从 0 开始）
     * @param size    每页大小
     * @return 当前页的 Key 列表
     */
    public static List<String> scanKeysForPage(String pattern, int page, int size) {
        RedisConnectionFactory factory = getRedisTemplate().getConnectionFactory();
        Objects.requireNonNull(factory, "RedisConnectionFactory must not be null");

        RedisConnection connection = factory.getConnection();
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
            Cursor<byte[]> cursor = connection.keyCommands().scan(options);

            // 计算当前页在全局迭代结果中的起止下标
            int fromIndex = page * size;
            int toIndex = fromIndex + size;
            List<String> result = new ArrayList<>(size);
            int index = 0;

            while (cursor.hasNext()) {
                // 已收集足够条目，提前终止 SCAN 迭代
                if (index >= toIndex) {
                    break;
                }
                byte[] keyBytes = cursor.next();
                if (index >= fromIndex) {
                    result.add(new String(keyBytes, StandardCharsets.UTF_8));
                }
                index++;
            }
            return result;
        } finally {
            RedisConnectionUtils.releaseConnection(connection, factory);
        }
    }

    // ======================== String 操作 ========================

    /**
     * 获取 String 类型的值
     *
     * @param key Redis Key
     * @param <T> 值类型
     * @return 值，不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) getRedisTemplate().opsForValue().get(key);
    }

    /**
     * 批量获取 String 类型的值
     *
     * @param keys Key 列表
     * @param <T>  值类型
     * @return 值列表，不存在的 Key 对应位置为 null
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> multiGet(List<String> keys) {
        return (List<T>) getRedisTemplate().opsForValue().multiGet(keys);
    }

    /**
     * 存入 String 类型的值
     *
     * @param key   Redis Key
     * @param value 值
     */
    public static void set(String key, Object value) {
        getRedisTemplate().opsForValue().set(key, value);
    }

    /**
     * 存入 String 类型的值并设置过期时间
     *
     * @param key     Redis Key
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public static void set(String key, Object value, long timeout, TimeUnit unit) {
        getRedisTemplate().opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 仅在 Key 不存在时设置值（原子操作）
     *
     * @param key     Redis Key
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return true 表示写入成功，false 表示 Key 已存在
     */
    public static Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return getRedisTemplate().opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    /**
     * 对数值类型的 Key 执行自增操作
     *
     * @param key   Redis Key
     * @param delta 自增步长（可为负数）
     * @return 自增后的值
     */
    public static long increment(String key, long delta) {
        Long result = getRedisTemplate().opsForValue().increment(key, delta);
        return result != null ? result : 0;
    }

    // ======================== Hash 操作 ========================

    /**
     * 获取 Hash 中指定字段的值
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段名
     * @param <HK>    Hash 字段类型
     * @param <HV>    Hash 值类型
     * @return 字段值，不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public static <HK, HV> HV hGet(String key, HK hashKey) {
        return (HV) getRedisTemplate().opsForHash().get(key, hashKey);
    }

    /**
     * 获取 Hash 的全部字段
     *
     * @param key  Redis Key
     * @param <HK> Hash 字段类型
     * @param <HV> Hash 值类型
     * @return 字段名-值映射
     */
    @SuppressWarnings("unchecked")
    public static <HK, HV> Map<HK, HV> hGetAll(String key) {
        return (Map<HK, HV>) getRedisTemplate().opsForHash().entries(key);
    }

    /**
     * 向 Hash 中存入单个字段
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段名
     * @param value   字段值
     */
    public static void hPut(String key, String hashKey, Object value) {
        getRedisTemplate().opsForHash().put(key, hashKey, value);
    }

    /**
     * 批量向 Hash 中存入字段
     *
     * @param key Redis Key
     * @param map 字段名-值映射
     */
    public static void hPutAll(String key, Map<String, Object> map) {
        getRedisTemplate().opsForHash().putAll(key, map);
    }

    /**
     * 删除 Hash 中的一个或多个字段
     *
     * @param key      Redis Key
     * @param hashKeys 字段名，可多个
     * @return 删除的字段数量
     */
    public static Long hDelete(String key, Object... hashKeys) {
        return getRedisTemplate().opsForHash().delete(key, hashKeys);
    }

    /**
     * 判断 Hash 中是否存在指定字段
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段名
     * @return true 表示存在
     */
    public static boolean hHasKey(String key, String hashKey) {
        return Boolean.TRUE.equals(getRedisTemplate().opsForHash().hasKey(key, hashKey));
    }

    /**
     * 对 Hash 中数值字段执行自增（长整型）
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段名
     * @param delta   自增步长（可为负数）
     * @return 自增后的值
     */
    public static long hIncr(String key, String hashKey, long delta) {
        return getRedisTemplate().opsForHash().increment(key, hashKey, delta);
    }

    /**
     * 对 Hash 中数值字段执行自增（浮点型）
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段名
     * @param delta   自增步长（可为负数）
     * @return 自增后的值
     */
    public static double hIncr(String key, String hashKey, double delta) {
        return getRedisTemplate().opsForHash().increment(key, hashKey, delta);
    }

    // ======================== Set 操作 ========================

    /**
     * 获取 Set 的全部元素
     *
     * @param key Redis Key
     * @param <T> 元素类型
     * @return 元素集合
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> sMembers(String key) {
        return (Set<T>) getRedisTemplate().opsForSet().members(key);
    }

    /**
     * 判断元素是否在 Set 中
     *
     * @param key   Redis Key
     * @param value 元素值
     * @return true 表示存在
     */
    public static boolean sIsMember(String key, Object value) {
        return Boolean.TRUE.equals(getRedisTemplate().opsForSet().isMember(key, value));
    }

    /**
     * 向 Set 中添加元素
     *
     * @param key    Redis Key
     * @param values 元素值，可多个
     * @return 实际添加的元素数量
     */
    public static long sAdd(String key, Object... values) {
        Long count = getRedisTemplate().opsForSet().add(key, values);
        return count != null ? count : 0;
    }

    /**
     * 获取 Set 的元素数量
     *
     * @param key Redis Key
     * @return Set 大小
     */
    public static long sSize(String key) {
        Long size = getRedisTemplate().opsForSet().size(key);
        return size != null ? size : 0;
    }

    /**
     * 从 Set 中移除元素
     *
     * @param key    Redis Key
     * @param values 元素值，可多个
     * @return 实际移除的元素数量
     */
    public static long sRemove(String key, Object... values) {
        Long count = getRedisTemplate().opsForSet().remove(key, values);
        return count != null ? count : 0;
    }

    /**
     * 获取两个 Set 的差集
     *
     * @param key      Redis Key
     * @param otherKey 另一个 Set 的 Key
     * @param <T>      元素类型
     * @return 差集元素集合
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> sDifference(String key, String otherKey) {
        return (Set<T>) getRedisTemplate().opsForSet().difference(key, otherKey);
    }

    // ======================== List 操作 ========================

    /**
     * 获取 List 指定范围的元素
     *
     * @param key   Redis Key
     * @param start 起始下标（含）
     * @param end   结束下标（含，-1 表示尾部）
     * @param <T>   元素类型
     * @return 元素列表
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> lRange(String key, long start, long end) {
        return (List<T>) getRedisTemplate().opsForList().range(key, start, end);
    }

    /**
     * 获取 List 的长度
     *
     * @param key Redis Key
     * @return List 长度
     */
    public static long lSize(String key) {
        Long size = getRedisTemplate().opsForList().size(key);
        return size != null ? size : 0;
    }

    /**
     * 通过索引获取 List 中的元素
     *
     * @param key   Redis Key
     * @param index 索引（0 表示头部，-1 表示尾部）
     * @param <T>   元素类型
     * @return 元素值，不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T lIndex(String key, long index) {
        return (T) getRedisTemplate().opsForList().index(key, index);
    }

    /**
     * 向 List 左端（头部）插入元素
     *
     * @param key   Redis Key
     * @param value 元素值
     */
    public static void lLeftPush(String key, Object value) {
        getRedisTemplate().opsForList().leftPush(key, value);
    }

    /**
     * 向 List 右端（尾部）插入元素
     *
     * @param key   Redis Key
     * @param value 元素值
     */
    public static void lRightPush(String key, Object value) {
        getRedisTemplate().opsForList().rightPush(key, value);
    }

    /**
     * 修改 List 中指定索引的元素值
     *
     * @param key   Redis Key
     * @param index 索引
     * @param value 新值
     */
    public static void lSet(String key, long index, Object value) {
        getRedisTemplate().opsForList().set(key, index, value);
    }

    /**
     * 从 List 中移除指定数量的元素
     *
     * @param key   Redis Key
     * @param count 移除数量（正数从头移除，负数从尾移除）
     * @param value 元素值
     * @return 实际移除的数量
     */
    public static long lRemove(String key, long count, Object value) {
        Long removed = getRedisTemplate().opsForList().remove(key, count, value);
        return removed != null ? removed : 0;
    }

    // ======================== ZSet 操作 ========================

    /**
     * 获取 ZSet 指定范围的元素（按 score 升序）
     *
     * @param key   Redis Key
     * @param start 起始下标
     * @param end   结束下标
     * @param <T>   元素类型
     * @return 元素集合
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> zRange(String key, long start, long end) {
        return (Set<T>) getRedisTemplate().opsForZSet().range(key, start, end);
    }

    /**
     * 获取 ZSet 指定范围的元素（按 score 降序）
     *
     * @param key   Redis Key
     * @param start 起始下标
     * @param end   结束下标
     * @param <T>   元素类型
     * @return 元素集合
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> zReverseRange(String key, long start, long end) {
        return (Set<T>) getRedisTemplate().opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取 ZSet 中指定 score 范围内的元素
     *
     * @param key Redis Key
     * @param min 最小 score（含）
     * @param max 最大 score（含）
     * @param <T> 元素类型
     * @return 元素集合
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> zRangeByScore(String key, double min, double max) {
        return (Set<T>) getRedisTemplate().opsForZSet().rangeByScore(key, min, max);
    }

    /**
     * 获取 ZSet 的元素数量
     *
     * @param key Redis Key
     * @return ZSet 大小
     */
    public static long zSize(String key) {
        Long size = getRedisTemplate().opsForZSet().size(key);
        return size != null ? size : 0;
    }

    // ======================== 通用执行 ========================

    /**
     * 执行 Redis 命令回调
     *
     * @param callback Redis 回调
     * @param <T>      返回类型
     * @return 执行结果
     */
    public static <T> T execute(RedisCallback<T> callback) {
        return getRedisTemplate().execute(callback);
    }
}
