package cn.qijiv.domain.strategy.repository;

import cn.qijiv.domain.strategy.model.StrategyAwardEntity;

import java.util.List;
import java.util.Map;

/**
 * 抽奖策略仓库
 *
 * @author jinlujia
 * @date 2026/07/18
 */
public interface IStrategyRepository {

    /**
     * 查询抽奖策略奖品列表
     *
     * @param strategyId 抽奖策略ID
     * @return 抽奖策略奖品列表
     */
    List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId);

    /**
     * 存储策略奖品概率查找表
     *
     * @param strategyId 策略ID
     * @param rateTable  概率查找表（key: 随机值, value: 奖品ID）
     */
    void storeStrategyRateTable(Long strategyId, Map<Integer, Integer> rateTable);

    /**
     * 存储概率范围（百分位/千分位/万分位）
     *
     * @param strategyId 策略ID
     * @param rateRange  概率范围
     */
    void storeStrategyRateRange(Long strategyId, Integer rateRange);

}
