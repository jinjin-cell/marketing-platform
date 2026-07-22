package cn.qijiv.domain.strategy.service.rule.chain;

/**
 * 抽奖前置规则责任链节点。
 *
 * <p>节点有两种处理结果：规则命中时直接返回奖品 ID，规则不满足时调用下一个节点。</p>
 */
public interface ILogicChain extends ILogicChainArmory {

    /**
     * 执行当前节点。
     *
     * @param userId 用户ID
     * @param strategyId 策略ID
     * @return 奖品ID；节点接管抽奖或默认节点完成抽奖后返回
     */
    Integer logic(String userId, Long strategyId);
}
