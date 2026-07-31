package cn.qijiv.infrastructure.persistent.redis;

import cn.qijiv.types.common.Constants;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 订单业务号布隆过滤器。过滤器保存数据库业务键的超集：假阳性允许，假阴性不允许。
 */
@Component
public class OrderBusinessNoBloomFilter {

    private final RedissonClient redissonClient;
    private final long expectedInsertions;
    private final double falseProbability;

    public OrderBusinessNoBloomFilter(
            RedissonClient redissonClient,
            @Value("${activity.order.bloom-filter.expected-insertions:1000000}") long expectedInsertions,
            @Value("${activity.order.bloom-filter.false-probability:0.001}") double falseProbability) {
        this.redissonClient = redissonClient;
        this.expectedInsertions = expectedInsertions;
        this.falseProbability = falseProbability;
    }

    public void initialize() {
        bloomFilter().tryInit(expectedInsertions, falseProbability);
    }

    public boolean mightContain(String userId, String outBusinessNo) {
        RBloomFilter<String> bloomFilter = bloomFilter();
        ensureInitialized(bloomFilter);
        return bloomFilter.contains(toBusinessKey(userId, outBusinessNo));
    }

    /**
     * 必须在数据库事务之前调用。数据库写入失败时残留的是允许的假阳性。
     */
    public void add(String userId, String outBusinessNo) {
        RBloomFilter<String> bloomFilter = bloomFilter();
        ensureInitialized(bloomFilter);
        bloomFilter.add(toBusinessKey(userId, outBusinessNo));
    }

    private RBloomFilter<String> bloomFilter() {
        return redissonClient.getBloomFilter(
                Constants.RedisKey.ACTIVITY_ORDER_BUSINESS_NO_BLOOM_FILTER,
                StringCodec.INSTANCE
        );
    }

    private void ensureInitialized(RBloomFilter<String> bloomFilter) {
        if (!bloomFilter.isExists()) {
            throw new IllegalStateException("订单业务号布隆过滤器未初始化，拒绝产生假阴性");
        }
    }

    private String toBusinessKey(String userId, String outBusinessNo) {
        return userId.length() + ":" + userId + ":" + outBusinessNo;
    }
}
