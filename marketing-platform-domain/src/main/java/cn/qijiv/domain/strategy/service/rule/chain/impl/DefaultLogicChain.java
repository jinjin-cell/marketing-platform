package cn.qijiv.domain.strategy.service.rule.chain.impl;

import cn.qijiv.domain.strategy.service.armory.IStrategyDispatch;
import cn.qijiv.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.qijiv.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** 默认概率抽奖责任链节点。 */
@Slf4j
@Component("default")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DefaultLogicChain extends AbstractLogicChain {

    /** 策略抽奖调度服务，负责执行普通概率抽奖。 */
    private final IStrategyDispatch strategyDispatch;

    /**
     * 注入策略抽奖调度服务。
     *
     * @param strategyDispatch 策略抽奖调度服务
     */
    public DefaultLogicChain(IStrategyDispatch strategyDispatch) {
        this.strategyDispatch = strategyDispatch;
    }

    /**
     * 执行默认概率抽奖，作为责任链的兜底节点。
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @return 责任链抽奖结果，为普通概率抽中的奖品
     */
    @Override
    public DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        // 前置规则都未接管时，由默认节点执行策略普通概率抽奖。
        Integer awardId = strategyDispatch.getRandomAwardId(strategyId);
        log.info("抽奖责任链-默认抽奖 userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);
        return DefaultChainFactory.StrategyAwardVO.builder()
                .awardId(awardId)
                .logicModel(DefaultChainFactory.DEFAULT_CHAIN)
                .build();
    }

    /**
     * 返回本节点的规则模型名称。
     *
     * @return 规则模型名称，即 {@code default}
     */
    @Override
    protected String ruleModel() {
        return "default";
    }
}
