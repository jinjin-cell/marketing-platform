package cn.qijiv.infrastructure.persistent.db;

import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;

import java.util.Collection;

/**
 * 根据用户 ID 路由活动订单与账户，避免旧版 Groovy 内联算法在高版本 JDK 上的反射兼容问题。
 */
public class UserIdShardingAlgorithm implements PreciseShardingAlgorithm<String> {

    private static final int DATABASE_COUNT = 2;
    private static final int ORDER_TABLE_COUNT = 4;

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<String> shardingValue) {
        String userId = shardingValue.getValue();
        if (null == userId) {
            throw new IllegalArgumentException("user_id cannot be null");
        }

        int hash = userId.hashCode();
        String database = "ds" + (Math.floorMod(hash, DATABASE_COUNT) + 1);
        if (availableTargetNames.contains(database)) {
            return database;
        }

        String table = shardingValue.getLogicTableName() + "_00" + Math.floorMod(hash, ORDER_TABLE_COUNT);
        if (availableTargetNames.contains(table)) {
            return table;
        }

        throw new IllegalArgumentException("No sharding target for user_id: " + userId);
    }
}
