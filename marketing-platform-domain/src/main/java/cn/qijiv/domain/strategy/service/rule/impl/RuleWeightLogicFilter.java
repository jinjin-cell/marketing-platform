package cn.qijiv.domain.strategy.service.rule.impl;

import cn.qijiv.domain.strategy.model.entity.RuleActionEntity;
import cn.qijiv.domain.strategy.model.entity.RuleMatterEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.annotation.LogicStrategy;
import cn.qijiv.domain.strategy.service.rule.ILogicFilter;
import cn.qijiv.domain.strategy.service.rule.factory.DefaultLogicFactory;
import cn.qijiv.types.common.Constants;
import org.springframework.stereotype.Component;

/** 用户积分权重前置规则。 */
@Component
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_WEIGHT)
public class RuleWeightLogicFilter implements ILogicFilter<RuleActionEntity.RaffleBeforeEntity> {

    private final IStrategyRepository repository;

    public RuleWeightLogicFilter(IStrategyRepository repository) {
        this.repository = repository;
    }

    @Override
    public RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> filter(RuleMatterEntity ruleMatterEntity) {
        StrategyRuleEntity rule = repository.queryStrategyRule(
                ruleMatterEntity.getStrategyId(), ruleMatterEntity.getRuleModel());
        if (rule == null) {
            throw new IllegalArgumentException("权重规则配置不存在");
        }

        Long userScore = queryUserScore(ruleMatterEntity.getUserId());
        if (userScore == null) {
            return allow();
        }

        // 在全部“积分门槛:奖品范围”中选择用户能够满足的最高门槛。
        // 不能按升序取第一个，否则高积分用户会一直落入最低档位。
        String matchedRuleWeightValue = null;
        long matchedThreshold = Long.MIN_VALUE;
        for (String ruleWeightValue : rule.getRuleWeightValues().keySet()) {
            int colonIndex = ruleWeightValue.indexOf(Constants.COLON);
            if (colonIndex <= 0) {
                throw new IllegalArgumentException("rule_weight invalid input format: " + ruleWeightValue);
            }
            long threshold = Long.parseLong(ruleWeightValue.substring(0, colonIndex).trim());
            if (userScore >= threshold && threshold > matchedThreshold) {
                matchedThreshold = threshold;
                matchedRuleWeightValue = ruleWeightValue;
            }
        }

        if (matchedRuleWeightValue == null) {
            // 未达到最低门槛时继续使用完整奖池。
            return allow();
        }

        // 返回完整配置字符串，确保与策略装配阶段生成的 Redis 概率表 key 一致。
        return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                .ruleModel(DefaultLogicFactory.LogicModel.RULE_WEIGHT.getCode())
                .data(RuleActionEntity.RaffleBeforeEntity.builder()
                        .strategyId(ruleMatterEntity.getStrategyId())
                        .ruleWeightValueKey(matchedRuleWeightValue)
                        .build())
                .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                .build();
    }

    /**
     * 当前尚未接入用户积分账户，暂使用文档中的示例积分。
     * 后续接入账户仓储时只需替换此查询点。
     */
    protected Long queryUserScore(String userId) {
        return 4500L;
    }

    private RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> allow() {
        return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                .ruleModel(DefaultLogicFactory.LogicModel.RULE_WEIGHT.getCode())
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }
}
