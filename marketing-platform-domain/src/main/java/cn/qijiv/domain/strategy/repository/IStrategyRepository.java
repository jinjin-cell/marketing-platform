package cn.qijiv.domain.strategy.repository;

import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;

import java.util.List;

/**
 * 抽奖策略仓库
 *
 * @author jinlujia
 * @since 2026-07-18
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
     * @param strategyId 策略表业务key，可以是策略ID或策略ID+权重配置
     * @param rateTable  概率查找表，列表下标为随机值，元素为奖品ID
     */
    void storeStrategyRateTable(String strategyId, List<Integer> rateTable);

    /**
     * 查询策略概率表的槽位数量，作为随机数上界
     *
     * @param strategyKey 策略表业务key
     * @return 概率表槽位数量
     */
    Integer queryStrategyRateTableSize(String strategyKey);

    /**
     * 按随机值查询策略奖品 ID
     *
     * @param strategyKey 策略表业务key
     * @param randomValue 概率表下标
     * @return 奖品ID
     */
    Integer queryStrategyAwardId(String strategyKey, Integer randomValue);

    /**
     * 查询策略实体
     *
     * @param strategyId 策略ID
     * @return 策略实体
     */
    StrategyEntity queryStrategyEntityByStrategyId(Long strategyId);

    /**
     * 查询策略规则
     *
     * @param strategyId 策略ID
     * @param ruleModel  规则模型
     * @return 策略规则实体
     */
    StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel);

}
