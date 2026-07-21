package cn.qijiv.domain.strategy.service.rule.impl;

import cn.qijiv.domain.strategy.model.entity.RuleActionEntity;
import cn.qijiv.domain.strategy.model.entity.RuleMatterEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.annotation.LogicStrategy;
import cn.qijiv.domain.strategy.service.rule.ILogicFilter;
import cn.qijiv.domain.strategy.service.rule.factory.DefaultLogicFactory;
import cn.qijiv.types.common.Constants; 
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/** 黑名单前置规则。 */
@Component
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_BLACKLIST)
public class RuleBackListLogicFilter implements ILogicFilter<RuleActionEntity.RaffleBeforeEntity> {

    private final IStrategyRepository repository;

    public RuleBackListLogicFilter(IStrategyRepository repository) {
        this.repository = repository;
    }

    /**
     * 执行黑名单规则检查
     *
     * @param ruleMatterEntity 抽奖因子
     * @return 抽奖前逻辑检查结果
     */
    @Override
    public RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> filter(RuleMatterEntity ruleMatterEntity) {
        StrategyRuleEntity rule = repository.queryStrategyRule(
                ruleMatterEntity.getStrategyId(), ruleMatterEntity.getRuleModel());
        if (rule == null || StringUtils.isBlank(rule.getRuleValue())) {
            throw new IllegalArgumentException("黑名单规则配置不存在");
        }

        // 配置格式为“固定奖品ID:用户ID,用户ID”，冒号左侧用于规则接管后的直接返回。
        String[] ruleValueParts = rule.getRuleValue().split(Constants.COLON, 2);
        if (ruleValueParts.length != 2
                || StringUtils.isBlank(ruleValueParts[0])
                || StringUtils.isBlank(ruleValueParts[1])) {
            throw new IllegalArgumentException("rule_blacklist invalid input format: " + rule.getRuleValue());
        }

        Integer awardId = Integer.parseInt(ruleValueParts[0].trim());
        for (String blackUserId : ruleValueParts[1].split(Constants.SPLIT)) {
            if (ruleMatterEntity.getUserId().equals(blackUserId.trim())) {
                validateAward(ruleMatterEntity.getStrategyId(), awardId);
                // 命中黑名单后直接接管，不再进入权重规则或默认概率表。
                return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                        .ruleModel(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode())
                        .data(RuleActionEntity.RaffleBeforeEntity.builder()
                                .strategyId(ruleMatterEntity.getStrategyId())
                                .awardId(awardId)
                                .build())
                        .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                        .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                        .build();
            }
        }

        // 用户不在名单中时不改变后续抽奖路径。
        return allow();
    }

    private void validateAward(Long strategyId, Integer awardId) {
        List<StrategyAwardEntity> strategyAwards = repository.queryStrategyAwardList(strategyId);
        if (strategyAwards == null || strategyAwards.stream()
                .noneMatch(award -> award != null && awardId.equals(award.getAwardId()))) {
            throw new IllegalArgumentException(
                    "黑名单规则指定的奖品不属于当前策略，strategyId: " + strategyId + ", awardId: " + awardId);
        }
    }

    private RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> allow() {
        return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                .ruleModel(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode())
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }
}
