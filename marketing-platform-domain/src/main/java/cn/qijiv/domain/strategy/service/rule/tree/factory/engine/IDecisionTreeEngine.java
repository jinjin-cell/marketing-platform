package cn.qijiv.domain.strategy.service.rule.tree.factory.engine;

import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;

import java.util.Date;

/** 决策树执行引擎。 */
public interface IDecisionTreeEngine {

    /**
     * 从根节点开始执行规则树。
     *
     * @param userId      用户ID
     * @param strategyId  策略ID
     * @param awardId     奖品ID
     * @param endDateTime 活动结束时间，可为空；透传给库存类节点设置锁缓存有效期
     * @return 被规则接管后的奖品数据；未接管时返回 {@code null}
     */
    DefaultTreeFactory.StrategyAwardVO process(
            String userId, Long strategyId, Integer awardId, Date endDateTime);
}
