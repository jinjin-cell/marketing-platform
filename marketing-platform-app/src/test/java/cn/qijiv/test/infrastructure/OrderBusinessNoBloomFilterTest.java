package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.persistent.redis.OrderBusinessNoBloomFilter;
import cn.qijiv.types.common.Constants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrderBusinessNoBloomFilterTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RBloomFilter<String> bloomFilter;

    private OrderBusinessNoBloomFilter orderBloomFilter;

    @Before
    public void setUp() {
        when(redissonClient.<String>getBloomFilter(
                Constants.RedisKey.ACTIVITY_ORDER_BUSINESS_NO_BLOOM_FILTER,
                StringCodec.INSTANCE)).thenReturn(bloomFilter);
        orderBloomFilter = new OrderBusinessNoBloomFilter(redissonClient, 1000, 0.001);
    }

    @Test
    public void mightContain_usesUserScopedBusinessKey() {
        when(bloomFilter.isExists()).thenReturn(true);
        when(bloomFilter.contains("7:user001:business001")).thenReturn(true);

        assertTrue(orderBloomFilter.mightContain("user001", "business001"));
    }

    @Test
    public void add_registersBusinessKeyBeforeDatabaseWrite() {
        when(bloomFilter.isExists()).thenReturn(true);

        orderBloomFilter.add("user001", "business001");

        verify(bloomFilter).add("7:user001:business001");
    }

    @Test
    public void missingBloomFilter_failsClosedInsteadOfReturningAbsent() {
        when(bloomFilter.isExists()).thenReturn(false);

        try {
            orderBloomFilter.mightContain("user001", "business001");
            fail("Bloom 丢失时必须失败，不能把数据库已有订单误判为不存在");
        } catch (IllegalStateException expected) {
            // expected
        }
    }
}
