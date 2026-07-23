package cn.qijiv.domain.strategy.service.rule.tree.impl;

import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** 规则树中的次数解锁节点。 */
@Component("rule_lock")
public class RuleLockLogicTreeNode implements ILogicTreeNode {

    private final IStrategyRepository repository;

    public RuleLockLogicTreeNode(IStrategyRepository repository) {
        this.repository = repository;
    }

    @Override
    public DefaultTreeFactory.TreeActionEntity logic(
            String userId, Long strategyId, Integer awardId, String ruleValue) {
        // 同一棵树可以被多个奖品复用；奖品级 rule_lock 值优先覆盖节点默认值。
        StrategyRuleEntity awardRule = repository.queryStrategyAwardRule(
                strategyId, awardId, "rule_lock");
        String lockValue = awardRule != null && StringUtils.isNotBlank(awardRule.getRuleValue())
                ? awardRule.getRuleValue()
                : ruleValue;
        if (StringUtils.isBlank(lockValue)) {
            throw new IllegalArgumentException("次数锁节点未配置解锁次数");
        }
        final long requiredRaffleCount;
        try {
            requiredRaffleCount = Long.parseLong(lockValue.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("次数锁节点配置非法，ruleValue: " + lockValue, ex);
        }
        if (requiredRaffleCount <= 0) {
            throw new IllegalArgumentException("次数锁节点配置必须大于0，ruleValue: " + lockValue);
        }

        // 规则值来自 rule_tree_node，节点只关注“用户次数是否达到配置门槛”。
        Long userRaffleCount = queryUserRaffleCount(userId, strategyId);
        RuleLogicCheckTypeVO checkType = userRaffleCount != null
                && userRaffleCount >= requiredRaffleCount
                ? RuleLogicCheckTypeVO.ALLOW
                : RuleLogicCheckTypeVO.TAKE_OVER;
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(checkType)
                .build();
    }

    /**
     * 查询用户已经参与抽奖的次数。
     *
     * <p>当前项目还没有用户抽奖账户仓储，因此先保留清晰的扩展点。后续接入数据库或
     * Redis 后，只需要替换这里，不需要改动模板方法和规则树引擎。</p>
     */
    protected Long queryUserRaffleCount(String userId, Long strategyId) {
        return 0L;
    }
}
