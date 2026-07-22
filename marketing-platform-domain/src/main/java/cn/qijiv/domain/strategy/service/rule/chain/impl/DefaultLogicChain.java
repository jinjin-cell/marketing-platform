package cn.qijiv.domain.strategy.service.rule.chain.impl;

import cn.qijiv.domain.strategy.service.armory.IStrategyDispatch;
import cn.qijiv.domain.strategy.service.rule.chain.AbstractLogicChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** 默认概率抽奖责任链节点。 */
@Slf4j
@Component("default")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DefaultLogicChain extends AbstractLogicChain {

    private final IStrategyDispatch strategyDispatch;

    public DefaultLogicChain(IStrategyDispatch strategyDispatch) {
        this.strategyDispatch = strategyDispatch;
    }

    @Override
    public Integer logic(String userId, Long strategyId) {
        // 前置规则都未接管时，由默认节点执行策略普通概率抽奖。
        Integer awardId = strategyDispatch.getRandomAwardId(strategyId);
        log.info("抽奖责任链-默认抽奖 userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);
        return awardId;
    }

    @Override
    protected String ruleModel() {
        return "default";
    }
}
