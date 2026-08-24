package cn.qijiv.domain.strategy.service.armory;

/**
 * 策略兵工厂接口 —— 负责策略数据装配
 *
 * @author jinlujia
 * @since 2026-07-18
 */
public interface IStrategyArmory {

    /**
     * 装配抽奖策略
     *
     * @param strategyId 策略ID
     * @return 装配结果
     */
    boolean assembleLotteryStrategy(Long strategyId);

    /**
     * 根据活动ID装配抽奖策略
     *
     * @param activityId 活动ID
     * @return 装配结果
     */
    boolean assembleLotteryStrategyByActivityId(Long activityId);
}
