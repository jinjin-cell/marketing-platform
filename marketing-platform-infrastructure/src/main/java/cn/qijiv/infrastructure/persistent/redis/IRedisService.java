package cn.qijiv.infrastructure.persistent.redis;

import java.util.concurrent.TimeUnit;

/**
 * Redis operations shared by infrastructure consumers.
 */
public interface IRedisService {

    <T> void setValue(String key, T value);

    <T> void setValue(String key, T value, long timeout, TimeUnit unit);

    <T> T getValue(String key);

    boolean isExists(String key);

    boolean delete(String key);

}
