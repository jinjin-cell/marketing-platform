package cn.qijiv.infrastructure.persistent.redis;

import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
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
        RList<T> list = redissonClient.getList(key);
        list.delete();
        list.addAll(values);
    }

    @Override
    public <T> T getListValue(String key, int index) {
        if (index < 0) {
            return null;
        }
        RList<T> list = redissonClient.getList(key);
        try {
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
