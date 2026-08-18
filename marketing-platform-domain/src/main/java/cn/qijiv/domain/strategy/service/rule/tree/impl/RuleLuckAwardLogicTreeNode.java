package cn.qijiv.domain.strategy.service.rule.tree.impl;

import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.qijiv.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/** 规则树中的兜底奖励节点。 */
@Slf4j
@Component("rule_luck_award")
public class RuleLuckAwardLogicTreeNode implements ILogicTreeNode {

    /**
     * 执行兜底奖励逻辑：直接返回配置的兜底奖品并接管抽奖。
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @param awardId    当前抽中的奖品ID
     * @param ruleValue  兜底奖品配置，格式为 奖品ID[:奖品规则配置]
     * @return 节点执行结果
     */
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId, String ruleValue) {
    log.info("规则过滤-兜底奖品 userId:{} strategyId:{} awardId:{} ruleValue:{}", userId, strategyId, awardId, ruleValue);
    String[] split = ruleValue.split(Constants.COLON);
    if (split.length == 0) {
        log.error("规则过滤-兜底奖品，兜底奖品未配置告警 userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);
        throw new RuntimeException("兜底奖品未配置 " + ruleValue);
    }
    // 兜底奖励配置
    Integer luckAwardId = Integer.valueOf(split[0]);
    String awardRuleValue = split.length > 1 ? split[1] : "";
    // 返回兜底奖品
    log.info("规则过滤-兜底奖品 userId:{} strategyId:{} awardId:{} awardRuleValue:{}", userId, strategyId, luckAwardId, awardRuleValue);
    return DefaultTreeFactory.TreeActionEntity.builder()
            .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
            .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                    .awardId(luckAwardId)
                    .awardRuleValue(awardRuleValue)
                    .build())
            .build();
    }

}
