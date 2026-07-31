package cn.qijiv.infrastructure.persistent.redis;

import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis operations shared by infrastructure consumers.
 */
public interface IRedisService {

    <T> void setValue(String key, T value);

    <T> void setValue(String key, T value, long timeout, TimeUnit unit);

    <T> T getValue(String key);

    /** 使用 Redis List 保存可按下标访问的有序数据。 */
    <T> void setList(String key, List<T> values);

    /** 使用带过期时间的 Redis List 保存数据。 */
    <T> void setList(String key, List<T> values, long timeout, TimeUnit unit);

    /** 获取 Redis List 指定下标的单个元素。 */
    <T> T getListValue(String key, int index);

    /** 获取 Redis List 长度，对应抽奖概率表的随机数上界。 */
    int getListSize(String key);

    boolean isExists(String key);

    boolean delete(String key);

    long decr(String key);

    Long getAtomicLong(String cacheKey);

    void setAtomicLong(String cacheKey, Integer awardCount);

    boolean setAtomicLongIfAbsent(String cacheKey, Integer awardCount);

    Boolean setNx(String lockKey);

    <T> RBlockingQueue<T> getBlockingQueue(String cacheKey);

    <T> RDelayedQueue<T> getDelayedQueue(RBlockingQueue<T> blockingQueue);

    Boolean setNx(String key, long expired, TimeUnit timeUnit);

}
