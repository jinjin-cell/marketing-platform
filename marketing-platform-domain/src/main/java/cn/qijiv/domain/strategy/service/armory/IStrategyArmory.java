package cn.qijiv.domain.strategy.service.armory;

/**
 * 策略兵工厂接口 —— 负责策略数据装配
 *
 * @author jinlujia
 * @date 2026/07/18
 */
public interface IStrategyArmory {

    /**
     * 装配抽奖策略
     *
     * @param strategyId 策略ID
     * @return 装配结果
     */
    void assembleLotteryStrategy(Long strategyId);

    /**
     * 获取随机奖品ID
     *
     * @param strategyId 策略ID
     * @return 随机奖品ID
     */
    Integer getRandomAwardId(Long strategyId);

}
