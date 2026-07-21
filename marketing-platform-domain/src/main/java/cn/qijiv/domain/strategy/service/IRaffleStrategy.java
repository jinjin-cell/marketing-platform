package cn.qijiv.domain.strategy.service;

import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;

/**
 * 抽奖策略接口
 *
 * @author jinlujia
 * @date 2026/07/18
 */
public interface IRaffleStrategy {

    /**
     * 执行抽奖
     *
     * @param raffleFactorEntity 抽奖因子
     * @return 抽奖结果
     */
    RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity);

}
