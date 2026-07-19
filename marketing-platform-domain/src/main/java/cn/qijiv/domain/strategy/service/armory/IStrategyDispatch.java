package cn.qijiv.domain.strategy.service.armory;

/**
 * 策略抽奖调度接口，只负责使用已装配的概率表，不暴露装配操作。
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

}
