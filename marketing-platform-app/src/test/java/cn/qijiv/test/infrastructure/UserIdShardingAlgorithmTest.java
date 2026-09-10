package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.db.UserIdShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/** 用户 ID 分库分表算法单元测试：验证库表路由与边界场景。 */
public class UserIdShardingAlgorithmTest {

    /** 被测的分片算法实例。 */
    private final UserIdShardingAlgorithm algorithm = new UserIdShardingAlgorithm();

    /** 验证同一用户 ID 根据 hash 取模路由到一致的库与订单表。 */
    @Test
    public void routesDatabaseAndOrderTableBySameUserHash() {
        String userId = "order_flow_test_001";
        int hash = userId.hashCode();

        String database = algorithm.doSharding(
                Arrays.asList("ds1", "ds2"),
                new PreciseShardingValue<>("raffle_activity_order", "user_id", userId)
        );
        String table = algorithm.doSharding(
                Arrays.asList("raffle_activity_order_000", "raffle_activity_order_001",
                        "raffle_activity_order_002", "raffle_activity_order_003"),
                new PreciseShardingValue<>("raffle_activity_order", "user_id", userId)
        );

        assertEquals("ds" + (Math.floorMod(hash, 2) + 1), database);
        assertEquals("raffle_activity_order_00" + Math.floorMod(hash, 4), table);
    }

    /** 验证 Integer.MIN_VALUE 哈希不会产生负数路由。 */
    @Test
    public void handlesIntegerMinValueHashWithoutNegativeRoute() {
        String minHashUserId = "polygenelubricants";
        assertEquals(Integer.MIN_VALUE, minHashUserId.hashCode());

        String database = algorithm.doSharding(
                Arrays.asList("ds1", "ds2"),
                new PreciseShardingValue<>("raffle_activity_account", "user_id", minHashUserId)
        );
        String table = algorithm.doSharding(
                Arrays.asList("raffle_activity_order_000", "raffle_activity_order_001",
                        "raffle_activity_order_002", "raffle_activity_order_003"),
                new PreciseShardingValue<>("raffle_activity_order", "user_id", minHashUserId)
        );

        assertEquals("ds1", database);
        assertEquals("raffle_activity_order_000", table);
    }
}
