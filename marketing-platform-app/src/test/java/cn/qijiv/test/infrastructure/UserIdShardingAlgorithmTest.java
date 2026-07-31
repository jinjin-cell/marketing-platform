package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.persistent.db.UserIdShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class UserIdShardingAlgorithmTest {

    private final UserIdShardingAlgorithm algorithm = new UserIdShardingAlgorithm();

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
