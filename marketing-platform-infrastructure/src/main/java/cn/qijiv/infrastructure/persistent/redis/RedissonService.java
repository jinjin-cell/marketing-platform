package cn.qijiv.infrastructure.persistent.redis;

import org.redisson.api.RBucket;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redisson-backed implementation of the shared Redis service.
 */
@Service
public class RedissonService implements IRedisService {

    /** Redisson 客户端 */
    private final RedissonClient redissonClient;

    public RedissonService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 设置缓存值
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    @Override
    public <T> void setValue(String key, T value) {
        redissonClient.<T>getBucket(key).set(value);
    }

    /**
     * 设置带过期时间的缓存值
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    @Override
    public <T> void setValue(String key, T value, long timeout, TimeUnit unit) {
        redissonClient.<T>getBucket(key).set(value, timeout, unit);
    }

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存值，不存在时返回 null
     */
    @Override
    public <T> T getValue(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    /**
     * 覆盖式保存 Redis List 数据
     *
     * @param key    列表键
     * @param values 列表数据
     */
    @Override
    public <T> void setList(String key, List<T> values) {
        replaceList(key, values, null, null);
    }

    /**
     * 覆盖式保存带过期时间的 Redis List 数据
     *
     * @param key     列表键
     * @param values  列表数据
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    @Override
    public <T> void setList(String key, List<T> values, long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Redis List 过期时间必须大于0");
        }
        replaceList(key, values, timeout, Objects.requireNonNull(unit, "时间单位不能为空"));
    }

    /**
     * 以临时键原子替换的方式写入 Redis List，避免读取方看到写一半的中间状态
     *
     * @param key     目标列表键
     * @param values  列表数据
     * @param timeout 过期时间，可为 null
     * @param unit    时间单位
     */
    private <T> void replaceList(
            String key, List<T> values, Long timeout, TimeUnit unit) {
        if (values == null || values.isEmpty()) {
            redissonClient.getList(key).delete();
            return;
        }

        String temporaryKey = key + ":tmp:" + UUID.randomUUID();
        RList<T> temporaryList = redissonClient.getList(temporaryKey);
        boolean renamed = false;
        try {
            temporaryList.addAll(values);
            if (timeout != null && !temporaryList.expire(Duration.ofMillis(unit.toMillis(timeout)))) {
                throw new IllegalStateException("Redis List 设置过期时间失败，key: " + key);
            }
            // Redis RENAME 会原子替换旧key，读取方不会看到删除后尚未写完的中间状态。
            temporaryList.rename(key);
            renamed = true;
        } finally {
            if (!renamed) {
                redissonClient.getList(temporaryKey).delete();
            }
        }
    }

    /**
     * 获取 Redis List 指定下标的元素
     *
     * @param key   列表键
     * @param index 下标
     * @return 元素值，越界时返回 null
     */
    @Override
    public <T> T getListValue(String key, int index) {
        if (index < 0) {
            return null;
        }
        RList<T> list = redissonClient.getList(key);
        try {
            // Redisson 按下标读取，只传输命中的一个元素。
            return list.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * 获取 Redis List 长度
     *
     * @param key 列表键
     * @return 列表长度
     */
    @Override
    public int getListSize(String key) {
        return redissonClient.getList(key).size();
    }

    /**
     * 判断缓存键是否存在
     *
     * @param key 缓存键
     * @return 存在返回 true，否则返回 false
     */
    @Override
    public boolean isExists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    /**
     * 删除缓存键
     *
     * @param key 缓存键
     * @return 删除成功返回 true
     */
    @Override
    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    /**
     * 原子递减缓存值
     *
     * @param key 缓存键
     * @return 递减后的值
     */
    @Override
    public long decr(String key) {
        return redissonClient.getAtomicLong(key).decrementAndGet();
    }

    /**
     * 获取原子长整型缓存值
     *
     * @param cacheKey 缓存键
     * @return 当前值
     */
    @Override
    public Long getAtomicLong(String cacheKey) {
        return redissonClient.getAtomicLong(cacheKey).get();
    }

    /**
     * 设置原子长整型缓存值
     *
     * @param cacheKey   缓存键
     * @param awardCount 库存数量
     */
    @Override
    public void setAtomicLong(String cacheKey, Integer awardCount) {
        redissonClient.getAtomicLong(cacheKey).set(awardCount);
    }

    /**
     * 原子长整型值不存在时设置
     *
     * @param cacheKey   缓存键
     * @param awardCount 库存数量
     * @return 设置成功返回 true
     */
    @Override
    public boolean setAtomicLongIfAbsent(String cacheKey, Integer awardCount) {
        RBucket<Long> bucket = redissonClient.getBucket(cacheKey, LongCodec.INSTANCE);
        return bucket.setIfAbsent(awardCount.longValue());
    }

    /**
     * 获取分布式锁
     *
     * @param key 锁键
     * @return 获取成功返回 true
     */
    @Override
    public Boolean setNx(String key) {
        return redissonClient.getBucket(key).setIfAbsent("1");
    }

    /**
     * 获取阻塞队列
     *
     * @param cacheKey 队列键
     * @return 阻塞队列
     */
    @Override
    public <T> RBlockingQueue<T> getBlockingQueue(String cacheKey) {
        return redissonClient.getBlockingQueue(cacheKey);
    }

    /**
     * 获取延迟队列
     *
     * @param blockingQueue 目标阻塞队列
     * @return 延迟队列
     */
    @Override
    public <T> RDelayedQueue<T> getDelayedQueue(RBlockingQueue<T> blockingQueue) {
        return redissonClient.getDelayedQueue(blockingQueue);
    }

    /**
     * 获取带过期时间的分布式锁
     *
     * @param key      锁键
     * @param expired  过期时间
     * @param timeUnit 时间单位
     * @return 获取成功返回 true
     */
    @Override
    public Boolean setNx(String key, long expired, TimeUnit timeUnit) {
        return redissonClient.getBucket(key).setIfAbsent("lock", Duration.ofMillis(timeUnit.toMillis(expired)));
    }

    /**
     * 获取分布式可重入锁
     *
     * @param lockKey 锁键
     * @return 分布式锁
     */
    @Override
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

}
