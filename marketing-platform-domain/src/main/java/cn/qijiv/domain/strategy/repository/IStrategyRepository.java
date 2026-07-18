package cn.qijiv.domain.strategy.repository;

import cn.qijiv.domain.strategy.model.StrategyAwardEntity;

import java.util.List;

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
     * @param rateTable  概率查找表，列表下标为随机值，元素为奖品ID
     */
    void storeStrategyRateTable(Long strategyId, List<Integer> rateTable);

    /**
     * 查询策略概率表的槽位数量，作为随机数上界
     *
     * @param strategyId 策略ID
     * @return 概率表槽位数量
     */
    Integer queryStrategyRateTableSize(Long strategyId);

    /**
     * 按随机值查询策略奖品 ID
     *
     * @param strategyId  策略ID
     * @param randomValue 概率表下标
     * @return 奖品ID
     */
    Integer queryStrategyAwardId(Long strategyId, Integer randomValue);

}
