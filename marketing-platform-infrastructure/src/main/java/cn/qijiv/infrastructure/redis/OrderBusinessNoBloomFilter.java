package cn.qijiv.infrastructure.redis;

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

    /** Redisson 客户端 */
    private final RedissonClient redissonClient;
    /** 预期插入元素数量 */
    private final long expectedInsertions;
    /** 期望误判率 */
    private final double falseProbability;

    public OrderBusinessNoBloomFilter(
            RedissonClient redissonClient,
            @Value("${activity.order.bloom-filter.expected-insertions:1000000}") long expectedInsertions,
            @Value("${activity.order.bloom-filter.false-probability:0.001}") double falseProbability) {
        this.redissonClient = redissonClient;
        this.expectedInsertions = expectedInsertions;
        this.falseProbability = falseProbability;
    }

    /**
     * 初始化布隆过滤器容量与误判率参数
     */
    public void initialize() {
        bloomFilter().tryInit(expectedInsertions, falseProbability);
    }

    /**
     * 判断业务号是否可能已存在
     *
     * @param userId        用户ID
     * @param outBusinessNo 外部业务号
     * @return 可能存在返回 true，允许少量误判
     */
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

    /**
     * 获取订单业务号布隆过滤器实例
     */
    private RBloomFilter<String> bloomFilter() {
        return redissonClient.getBloomFilter(
                Constants.RedisKey.ACTIVITY_ORDER_BUSINESS_NO_BLOOM_FILTER,
                StringCodec.INSTANCE
        );
    }

    /**
     * 校验布隆过滤器已初始化，避免产生假阴性
     *
     * @param bloomFilter 布隆过滤器
     */
    private void ensureInitialized(RBloomFilter<String> bloomFilter) {
        if (!bloomFilter.isExists()) {
            throw new IllegalStateException("订单业务号布隆过滤器未初始化，拒绝产生假阴性");
        }
    }

    /**
     * 拼接业务键，防止不同用户的外部业务号相互冲突
     *
     * @param userId        用户ID
     * @param outBusinessNo 外部业务号
     * @return 拼接后的业务键
     */
    private String toBusinessKey(String userId, String outBusinessNo) {
        return userId.length() + ":" + userId + ":" + outBusinessNo;
    }
}
