package cn.qijiv.domain.strategy.service.rule.chain.impl;

import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.armory.IStrategyDispatch;
import cn.qijiv.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.qijiv.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import cn.qijiv.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** 积分权重抽奖责任链节点。 */
@Slf4j
@Component("rule_weight")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RuleWeightLogicChain extends AbstractLogicChain {

    private final IStrategyRepository repository;
    private final IStrategyDispatch strategyDispatch;

    public RuleWeightLogicChain(IStrategyRepository repository, IStrategyDispatch strategyDispatch) {
        this.repository = repository;
        this.strategyDispatch = strategyDispatch;
    }

    /**
     * 根据用户积分选择最高可满足的权重档位。
     *
     * <p>如果用户没有达到任何门槛，则放行到默认节点；命中档位后直接返回权重抽奖结果。</p>
     */
    @Override
    public Integer logic(String userId, Long strategyId) {
        StrategyRuleEntity rule = repository.queryStrategyRule(strategyId, ruleModel());
        if (rule == null) {
            throw new IllegalArgumentException("权重规则配置不存在");
        }

        Long userScore = queryUserScore(userId);
        if (userScore == null) {
            return nextLogic(userId, strategyId);
        }

        String matchedRuleWeightValue = null;
        long matchedThreshold = Long.MIN_VALUE;
        for (String ruleWeightValue : rule.getRuleWeightValues().keySet()) {
            int colonIndex = ruleWeightValue.indexOf(Constants.COLON);
            if (colonIndex <= 0) {
                throw new IllegalArgumentException("rule_weight invalid input format: " + ruleWeightValue);
            }
            long threshold = Long.parseLong(ruleWeightValue.substring(0, colonIndex).trim());
            if (userScore >= threshold && threshold > matchedThreshold) {
                // 不依赖配置顺序，始终保留用户能够达到的最高积分门槛。
                matchedThreshold = threshold;
                matchedRuleWeightValue = ruleWeightValue;
            }
        }

        if (matchedRuleWeightValue == null) {
            // 没有达到任何权重门槛，交给默认概率表处理。
            return nextLogic(userId, strategyId);
        }

        // 权重规则命中后直接接管抽奖，不再执行默认概率抽奖。
        Integer awardId = strategyDispatch.getRandomAwardId(strategyId, matchedRuleWeightValue);
        log.info("抽奖责任链-权重接管 userId:{} strategyId:{} score:{} awardId:{}",
                userId, strategyId, userScore, awardId);
        return awardId;
    }

    /** 用户积分账户接入后替换此查询点。 */
    protected Long queryUserScore(String userId) {
        // 当前为示例固定值；正式接入用户积分账户后替换为真实查询。
        return 4500L;
    }

    @Override
    protected String ruleModel() {
        return DefaultLogicFactory.LogicModel.RULE_WEIGHT.getCode();
    }
}
