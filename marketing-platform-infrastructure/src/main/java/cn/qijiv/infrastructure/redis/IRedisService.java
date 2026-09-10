package cn.qijiv.infrastructure.redis;

import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis operations shared by infrastructure consumers.
 */
public interface IRedisService {

    /**
     * 设置缓存值
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    <T> void setValue(String key, T value);

    /**
     * 设置带过期时间的缓存值
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    <T> void setValue(String key, T value, long timeout, TimeUnit unit);

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存值，不存在时返回 null
     */
    <T> T getValue(String key);

    /** 使用 Redis List 保存可按下标访问的有序数据。 */
    <T> void setList(String key, List<T> values);

    /** 使用带过期时间的 Redis List 保存数据。 */
    <T> void setList(String key, List<T> values, long timeout, TimeUnit unit);

    /** 获取 Redis List 指定下标的单个元素。 */
    <T> T getListValue(String key, int index);

    /** 获取 Redis List 长度，对应抽奖概率表的随机数上界。 */
    int getListSize(String key);

    /**
     * 判断缓存键是否存在
     *
     * @param key 缓存键
     * @return 存在返回 true，否则返回 false
     */
    boolean isExists(String key);

    /**
     * 删除缓存键
     *
     * @param key 缓存键
     * @return 删除成功返回 true
     */
    boolean delete(String key);

    /**
     * 原子递减缓存值
     *
     * @param key 缓存键
     * @return 递减后的值
     */
    long decr(String key);

    /**
     * 获取原子长整型缓存值
     *
     * @param cacheKey 缓存键
     * @return 当前值
     */
    Long getAtomicLong(String cacheKey);

    /**
     * 设置原子长整型缓存值
     *
     * @param cacheKey   缓存键
     * @param awardCount 库存数量
     */
    void setAtomicLong(String cacheKey, Integer awardCount);

    /**
     * 原子长整型值不存在时设置
     *
     * @param cacheKey   缓存键
     * @param awardCount 库存数量
     * @return 设置成功返回 true
     */
    boolean setAtomicLongIfAbsent(String cacheKey, Integer awardCount);

    /**
     * 获取分布式锁
     *
     * @param lockKey 锁键
     * @return 获取成功返回 true
     */
    Boolean setNx(String lockKey);

    /**
     * 获取阻塞队列
     *
     * @param cacheKey 队列键
     * @return 阻塞队列
     */
    <T> RBlockingQueue<T> getBlockingQueue(String cacheKey);

    /**
     * 获取延迟队列
     *
     * @param blockingQueue 目标阻塞队列
     * @return 延迟队列
     */
    <T> RDelayedQueue<T> getDelayedQueue(RBlockingQueue<T> blockingQueue);

    /**
     * 获取带过期时间的分布式锁
     *
     * @param key      锁键
     * @param expired  过期时间
     * @param timeUnit 时间单位
     * @return 获取成功返回 true
     */
    Boolean setNx(String key, long expired, TimeUnit timeUnit);

    /**
     * 获取分布式可重入锁
     *
     * @param lockKey 锁键
     * @return 分布式锁
     */
    RLock getLock(String lockKey);

}
