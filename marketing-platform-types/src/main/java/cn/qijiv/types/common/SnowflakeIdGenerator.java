package cn.qijiv.types.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花算法 ID 生成器
 * <p>
 * 64 位 ID 结构：1位符号位 + 41位时间戳（毫秒） + 5位数据中心ID + 5位机器ID + 12位序列号。
 * 相比 {@code RandomStringUtils.randomNumeric} 随机数，雪花 ID 全局唯一、趋势递增，
 * 不会因唯一索引（如 task.message_id）发生碰撞。
 *
 * @author qijiv
 * @since 2026-08-19
 */
@Slf4j
@Component
public class SnowflakeIdGenerator {

    /** 起始时间戳（2020-01-01 00:00:00），用于计算相对时间，可用约 69 年 */
    private static final long START_TIMESTAMP = 1577808000000L;

    /** 机器 ID 所占位数 */
    private static final long WORKER_ID_BITS = 5L;
    /** 数据中心 ID 所占位数 */
    private static final long DATA_CENTER_ID_BITS = 5L;
    /** 序列号所占位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 支持的最大机器 ID */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    /** 支持的最大数据中心 ID */
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    /** 机器 ID 左移位数 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    /** 数据中心 ID 左移位数 */
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    /** 时间戳左移位数 */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;
    /** 序列号掩码 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 机器 ID */
    private final long workerId;
    /** 数据中心 ID */
    private final long dataCenterId;
    /** 上次生成 ID 的时间戳 */
    private long lastTimestamp = -1L;
    /** 同毫秒内的序列号 */
    private long sequence = 0L;

    public SnowflakeIdGenerator(
            @Value("${snowflake.worker-id:1}") long workerId,
            @Value("${snowflake.data-center-id:1}") long dataCenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId 超出范围 [0, " + MAX_WORKER_ID + "]: " + workerId);
        }
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("dataCenterId 超出范围 [0, " + MAX_DATA_CENTER_ID + "]: " + dataCenterId);
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
        log.info("雪花算法初始化 workerId:{} dataCenterId:{}", workerId, dataCenterId);
    }

    /**
     * 生成下一个全局唯一 ID
     *
     * @return 雪花 ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        // 时钟回拨，拒绝生成，防止产生重复 ID
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("时钟回拨，拒绝生成雪花ID。lastTimestamp: " + lastTimestamp + ", timestamp: " + timestamp);
        }
        if (timestamp == lastTimestamp) {
            // 同一毫秒内，序列号自增
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 序列号用完，等待下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 阻塞等待至下一毫秒
     *
     * @param lastTimestamp 上次生成 ID 的时间戳
     * @return 下一个毫秒的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

}
