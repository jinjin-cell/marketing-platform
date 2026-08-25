package cn.qijiv.domain.strategy.service;

import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;

import java.util.List;

/**
 * 抽奖奖品服务
 *
 * @author qijiv
 * @since 2026-07-18
 */
public interface IRaffleAward {



    /**
     * 根据策略ID查询抽奖奖品列表配置
     *
     * @param strategyId 策略ID
     * @return 奖品列表
     */
    List<StrategyAwardEntity> queryRaffleStrategyAwardList(Long strategyId);


    /**
     * 根据活动ID查询抽奖奖品列表
     *
     * @param activityId 活动ID
     * @return 抽奖奖品列表
     */
    List<StrategyAwardEntity> queryRaffleStrategyAwardListByActivityId(Long activityId);
}
