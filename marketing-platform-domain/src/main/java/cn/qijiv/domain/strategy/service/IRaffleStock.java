package cn.qijiv.domain.strategy.service;

import cn.qijiv.domain.strategy.model.valobj.StrategyAwardStockKeyVO;

/** 奖品库存异步落库服务。 */
public interface IRaffleStock {

    StrategyAwardStockKeyVO takeQueueValue() throws InterruptedException;

    void updateStrategyAwardStock(Long strategyId, Integer awardId);
}
