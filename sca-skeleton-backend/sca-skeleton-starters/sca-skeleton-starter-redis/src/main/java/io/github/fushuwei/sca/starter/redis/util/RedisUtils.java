package io.github.fushuwei.sca.starter.redis.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 通用工具类。
 * <p>
 * 封装常用的 String、Hash、List、Set、ZSet 操作，屏蔽 RedisTemplate 的操作细节，
 * 统一异常处理与空值检查，降低业务代码的使用成本。
 * <p>
 * 通过 Spring 注入使用：{@code @Autowired RedisUtils redisUtils}
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class RedisUtils {

    // 注入的 RedisTemplate，Key 为 String，Value 为 Object（Jackson JSON 序列化）
    private final RedisTemplate<String, Object> redisTemplate;

    // ======================== Key 操作 ========================

    /**
     * 判断 Key 是否存在。
     *
     * @param key Redis Key
     * @return true 表示存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 删除单个 Key。
     *
     * @param key Redis Key
     * @return true 表示删除成功
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 批量删除 Key。
     *
     * @param keys Key 集合
     * @return 实际删除的 Key 数量
     */
    public long delete(Collection<String> keys) {
        Long count = redisTemplate.delete(keys);
        return count != null ? count : 0;
    }

    /**
     * 设置 Key 的过期时间。
     *
     * @param key     Redis Key
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return true 表示设置成功
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /**
     * 获取 Key 的剩余过期时间（秒）。
     *
     * @param key Redis Key
     * @return 剩余秒数，永不过期返回 -1，Key 不存在返回 -2
     */
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : -2;
    }

    // ======================== String 操作 ========================

    /**
     * 存入字符串类型值。
     *
     * @param key   Redis Key
     * @param value 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 存入字符串类型值并设置过期时间。
     *
     * @param key     Redis Key
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取字符串类型值。
     *
     * @param key Redis Key
     * @return 值，不存在时返回 null
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 仅在 Key 不存在时设置值（原子操作，适合分布式幂等写入）。
     *
     * @param key     Redis Key
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return true 表示写入成功（Key 不存在），false 表示 Key 已存在
     */
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit));
    }

    /**
     * 对数值类型的 Key 执行自增操作。
     *
     * @param key   Redis Key
     * @param delta 自增步长（正数自增，负数自减）
     * @return 自增后的值
     */
    public long increment(String key, long delta) {
        Long result = redisTemplate.opsForValue().increment(key, delta);
        return result != null ? result : 0;
    }

    // ======================== Hash 操作 ========================

    /**
     * 存入 Hash 单个字段。
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段名
     * @param value   字段值
     */
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 批量存入 Hash 字段。
     *
     * @param key Redis Key
     * @param map 字段名-值映射
     */
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 获取 Hash 单个字段值。
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段名
     * @return 字段值，不存在时返回 null
     */
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取 Hash 全部字段。
     *
     * @param key Redis Key
     * @return 字段名-值映射
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除 Hash 中的字段。
     *
     * @param key      Redis Key
     * @param hashKeys 要删除的字段名，可多个
     * @return 删除的字段数量
     */
    public long hDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    // ======================== List 操作 ========================

    /**
     * 向 List 左端（头部）插入元素。
     *
     * @param key   Redis Key
     * @param value 元素值
     */
    public void lLeftPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 向 List 右端（尾部）插入元素。
     *
     * @param key   Redis Key
     * @param value 元素值
     */
    public void lRightPush(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 获取 List 指定范围的元素。
     *
     * @param key   Redis Key
     * @param start 起始下标（含，0 为头部）
     * @param end   结束下标（含，-1 为尾部）
     * @return 元素列表
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取 List 长度。
     *
     * @param key Redis Key
     * @return List 长度，Key 不存在时返回 0
     */
    public long lSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0;
    }

    // ======================== Set 操作 ========================

    /**
     * 向 Set 中添加元素。
     *
     * @param key    Redis Key
     * @param values 元素值，可多个
     * @return 实际添加的元素数量
     */
    public long sAdd(String key, Object... values) {
        Long count = redisTemplate.opsForSet().add(key, values);
        return count != null ? count : 0;
    }

    /**
     * 判断元素是否在 Set 中。
     *
     * @param key   Redis Key
     * @param value 元素值
     * @return true 表示存在
     */
    public boolean sIsMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    /**
     * 获取 Set 全部元素。
     *
     * @param key Redis Key
     * @return 元素集合
     */
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }
}
