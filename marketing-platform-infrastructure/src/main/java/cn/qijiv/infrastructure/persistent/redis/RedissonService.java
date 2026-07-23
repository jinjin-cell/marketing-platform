package cn.qijiv.infrastructure.persistent.redis;

import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redisson-backed implementation of the shared Redis service.
 */
@Service
public class RedissonService implements IRedisService {

    private final RedissonClient redissonClient;

    public RedissonService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> void setValue(String key, T value) {
        redissonClient.<T>getBucket(key).set(value);
    }

    @Override
    public <T> void setValue(String key, T value, long timeout, TimeUnit unit) {
        redissonClient.<T>getBucket(key).set(value, timeout, unit);
    }

    @Override
    public <T> T getValue(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    @Override
    public <T> void setList(String key, List<T> values) {
        replaceList(key, values, null, null);
    }

    @Override
    public <T> void setList(String key, List<T> values, long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Redis List 过期时间必须大于0");
        }
        replaceList(key, values, timeout, Objects.requireNonNull(unit, "时间单位不能为空"));
    }

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
            if (timeout != null && !temporaryList.expire(timeout, unit)) {
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

    @Override
    public int getListSize(String key) {
        return redissonClient.getList(key).size();
    }

    @Override
    public boolean isExists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    @Override
    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

}
