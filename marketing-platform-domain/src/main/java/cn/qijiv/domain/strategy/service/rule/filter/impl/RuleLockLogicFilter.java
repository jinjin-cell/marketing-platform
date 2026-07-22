package cn.qijiv.domain.strategy.service.rule.filter.impl;

import cn.qijiv.domain.strategy.model.entity.RuleActionEntity;
import cn.qijiv.domain.strategy.model.entity.RuleMatterEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.annotation.LogicStrategy;
import cn.qijiv.domain.strategy.service.rule.ILogicFilter;
import cn.qijiv.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** 抽奖中置次数解锁规则。 */
@Slf4j
@Component
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_LOCK)
public class RuleLockLogicFilter implements ILogicFilter<RuleActionEntity.RaffleCenterEntity> {

    private final IStrategyRepository repository;

    /** 用户当前抽奖次数，后续可从数据库或 Redis 读取。 */
    private Long userRaffleCount = 0L;

    public RuleLockLogicFilter(IStrategyRepository repository) {
        this.repository = repository;
    }

    /**
     * 执行抽奖中置次数解锁规则
     *
     * @param ruleMatterEntity 抽奖规则事项
     * @return 抽奖中置次数解锁规则结果
     */
    @Override
    public RuleActionEntity<RuleActionEntity.RaffleCenterEntity> filter(RuleMatterEntity ruleMatterEntity) {
        log.info("规则过滤-次数锁 userId:{} strategyId:{} awardId:{} ruleModel:{}",
                ruleMatterEntity.getUserId(),
                ruleMatterEntity.getStrategyId(),
                ruleMatterEntity.getAwardId(),
                ruleMatterEntity.getRuleModel());

        if (ruleMatterEntity.getStrategyId() == null
                || ruleMatterEntity.getAwardId() == null
                || StringUtils.isBlank(ruleMatterEntity.getRuleModel())) {
            throw new IllegalArgumentException("次数锁规则入参不能为空");
        }

        StrategyRuleEntity rule = repository.queryStrategyAwardRule(
                ruleMatterEntity.getStrategyId(),
                ruleMatterEntity.getAwardId(),
                ruleMatterEntity.getRuleModel());
        if (rule == null || StringUtils.isBlank(rule.getRuleValue())) {
            throw new IllegalArgumentException("次数锁规则配置不存在");
        }

        long raffleCount;
        try {
            raffleCount = Long.parseLong(rule.getRuleValue().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("次数锁规则配置非法，ruleValue: " + rule.getRuleValue(), ex);
        }
        if (raffleCount <= 0) {
            throw new IllegalArgumentException("次数锁规则配置必须大于0，ruleValue: " + rule.getRuleValue());
        }

        if (userRaffleCount != null && userRaffleCount >= raffleCount) {
            return allow();
        }
        return takeOver();
    }


    private RuleActionEntity<RuleActionEntity.RaffleCenterEntity> allow() {
        return RuleActionEntity.<RuleActionEntity.RaffleCenterEntity>builder()
                .ruleModel(DefaultLogicFactory.LogicModel.RULE_LOCK.getCode())
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }

    private RuleActionEntity<RuleActionEntity.RaffleCenterEntity> takeOver() {
        return RuleActionEntity.<RuleActionEntity.RaffleCenterEntity>builder()
                .ruleModel(DefaultLogicFactory.LogicModel.RULE_LOCK.getCode())
                .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                .build();
    }
}
