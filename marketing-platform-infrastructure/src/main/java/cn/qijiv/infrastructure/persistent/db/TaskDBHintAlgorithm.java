package cn.qijiv.infrastructure.persistent.db;

import org.apache.shardingsphere.api.sharding.hint.HintShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.hint.HintShardingValue;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务表数据库 Hint 分片算法
 * <p>
 * task 表在 ds1、ds2 中各自存在一张（不分表），定时任务补偿扫描时通过
 * HintManager 手动指定库索引进行路由，例如 setDBKey(1) 路由到 ds1。
 *
 * @author qijiv
 * @since 2026/8/19
 */
public class TaskDBHintAlgorithm implements HintShardingAlgorithm<String> {

    /**
     * 根据 Hint 中设置的库索引返回匹配的数据源
     *
     * @param availableTargetNames 可用的目标数据源集合
     * @param shardingValue        Hint 分片值（库索引，如 "1"、"2"）
     * @return 匹配的数据源集合
     */
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, HintShardingValue<String> shardingValue) {
        String dbIdx = String.valueOf(shardingValue.getValues().iterator().next());
        List<String> result = availableTargetNames.stream()
                .filter(name -> name.endsWith(dbIdx))
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            throw new IllegalArgumentException("No sharding target for db hint: " + dbIdx);
        }
        return result;
    }

}
