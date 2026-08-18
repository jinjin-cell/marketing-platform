package cn.qijiv.domain.strategy.service;

import cn.qijiv.domain.strategy.model.valobj.StrategyAwardStockKeyVO;

/** 奖品库存异步落库服务。 */
public interface IRaffleStock {

    /**
     * 从延迟队列取出一条已到期的库存扣减消息。
     *
     * @return 库存扣减消息
     * @throws InterruptedException 线程被中断时抛出
     */
    StrategyAwardStockKeyVO takeQueueValue() throws InterruptedException;

    /**
     * 将一次成功的 Redis 库存扣减同步到数据库。
     *
     * @param strategyId 策略ID
     * @param awardId    奖品ID
     */
    void updateStrategyAwardStock(Long strategyId, Integer awardId);
}
