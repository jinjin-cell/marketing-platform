package cn.qijiv.domain.strategy.service.rule.tree.factory.engine;

import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;

/** 决策树执行引擎。 */
public interface IDecisionTreeEngine {

    /**
     * 从根节点开始执行规则树。
     *
     * @return 被规则接管后的奖品数据；未接管时返回 {@code null}
     */
    DefaultTreeFactory.StrategyAwardVO process(String userId, Long strategyId, Integer awardId);
}
