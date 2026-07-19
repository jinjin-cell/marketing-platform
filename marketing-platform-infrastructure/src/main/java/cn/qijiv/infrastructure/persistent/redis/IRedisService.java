package cn.qijiv.infrastructure.persistent.redis;

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

    /** 获取 Redis List 指定下标的单个元素。 */
    <T> T getListValue(String key, int index);

    /** 获取 Redis List 长度，对应抽奖概率表的随机数上界。 */
    int getListSize(String key);

    boolean isExists(String key);

    boolean delete(String key);

}
