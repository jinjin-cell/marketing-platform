package cn.qijiv.domain.strategy.service.rule.chain;

import cn.qijiv.domain.strategy.service.rule.chain.factory.DefaultChainFactory;

/**
 * 抽奖前置规则责任链节点。
 *
 * <p>节点有两种处理结果：规则命中时直接返回抽奖结果，规则不满足时调用下一个节点。</p>
 */
public interface ILogicChain extends ILogicChainArmory {

    /**
     * 执行当前节点。
     *
     * @param userId 用户ID
     * @param strategyId 策略ID
     * @return 责任链抽奖结果，包含奖品ID以及最终接管本次抽奖的规则模型
     */
    DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId);
}
