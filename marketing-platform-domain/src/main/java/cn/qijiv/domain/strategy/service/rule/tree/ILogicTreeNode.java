package cn.qijiv.domain.strategy.service.rule.tree;

import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;

import java.util.Date;

/** 规则树业务节点。 */
public interface ILogicTreeNode {

    /**
     * 执行单个规则节点。
     *
     * @param userId      用户ID
     * @param strategyId  策略ID
     * @param awardId     当前抽中的奖品ID
     * @param ruleValue   节点配置值
     * @param endDateTime 活动结束时间，可为空；库存类节点用于设置锁缓存有效期
     * @return 节点执行结果
     */
    DefaultTreeFactory.TreeActionEntity logic(
            String userId, Long strategyId, Integer awardId, String ruleValue, Date endDateTime);
}
