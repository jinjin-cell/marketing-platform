package cn.qijiv.domain.strategy.service.armory;

/**
 * 策略抽奖调度接口，对调用方只暴露抽奖操作；概率表缺失时由实现负责恢复装配。
 */
public interface IStrategyDispatch {

    /**
     * 使用策略完整奖品范围抽奖。
     *
     * @param strategyId 策略ID
     * @return 奖品ID
     */
    Integer getRandomAwardId(Long strategyId);

    /**
     * 使用指定权重档位的奖品范围抽奖。
     *
     * @param strategyId      策略ID
     * @param ruleWeightValue 完整权重配置，例如 4000:102,103,104,105
     * @return 奖品ID
     */
    Integer getRandomAwardId(Long strategyId, String ruleWeightValue);



    /**
     * 减少抽奖奖品库存
     *
     * @param strategyId 策略ID
     * @param awardId    奖品ID
     * @return 是否成功
     */
    Boolean subtractionAwardStock(Long strategyId, Integer awardId);


}
