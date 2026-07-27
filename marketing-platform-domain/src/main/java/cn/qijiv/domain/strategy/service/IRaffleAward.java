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
     * 查询抽奖奖品列表
     *
     * @param strategyId 抽奖策略ID
     * @return 抽奖奖品列表
     */
    List<StrategyAwardEntity> queryRaffleStrategyAwardList(Long strategyId);
}
