package cn.qijiv.domain.strategy.service.rule.tree.impl;

import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** 规则树中的兜底奖励节点。 */
@Component("rule_luck_award")
public class RuleLuckAwardLogicTreeNode implements ILogicTreeNode {

    private static final Integer LUCK_AWARD_ID = 101;
    /** 兜底奖励规则在数据库中的规则模型名称。 */
    private static final String RULE_LUCK_AWARD = "rule_luck_award";

    private final IStrategyRepository repository;

    public RuleLuckAwardLogicTreeNode(IStrategyRepository repository) {
        this.repository = repository;
    }

    @Override
    public DefaultTreeFactory.TreeActionEntity logic(
            String userId, Long strategyId, Integer awardId, String ruleValue) {
        String awardRuleValue = ruleValue;
        if (StringUtils.isBlank(awardRuleValue)) {
            // 树节点未直接配置规则值时，回退到奖品维度的数据库规则配置。
            StrategyRuleEntity rule = repository.queryStrategyAwardRule(
                    strategyId,
                    awardId,
                    RULE_LUCK_AWARD);
            if (rule == null || StringUtils.isBlank(rule.getRuleValue())) {
                throw new IllegalStateException(
                        "兜底奖励规则配置不存在，strategyId: " + strategyId + ", awardId: " + awardId);
            }
            awardRuleValue = rule.getRuleValue().trim();
        }

        // 兜底节点直接接管本次抽奖，并返回固定的积分奖品及其随机范围。
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                        .awardId(LUCK_AWARD_ID)
                        .awardRuleValue(awardRuleValue)
                        .build())
                .build();
    }
}
