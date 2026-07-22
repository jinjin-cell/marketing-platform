package cn.qijiv.domain.strategy.service.raffle;

import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.RuleActionEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.IRaffleStrategy;
import cn.qijiv.domain.strategy.service.rule.chain.ILogicChain;
import cn.qijiv.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.qijiv.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 抽奖策略抽象类
 *
 * @author jinlujia
 * @since 2026-07-18
 */
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {

    protected final IStrategyRepository repository;
    protected final DefaultChainFactory chainFactory;

    protected AbstractRaffleStrategy(IStrategyRepository repository, DefaultChainFactory chainFactory) {
        this.repository = repository;
        this.chainFactory = chainFactory;
    }

    /**
     * 执行抽奖
     *
     * @param raffleFactorEntity 抽奖因子
     * @return 抽奖结果
     */
    @Override
    public RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity) {
        // 抽奖入口只接受完整的用户和策略信息，避免规则链中途再处理无效参数。
        if (raffleFactorEntity == null
                || raffleFactorEntity.getStrategyId() == null
                || StringUtils.isBlank(raffleFactorEntity.getUserId())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        String userId = raffleFactorEntity.getUserId();
        Long strategyId = raffleFactorEntity.getStrategyId();

        ILogicChain logicChain = chainFactory.openLogicChain(strategyId);
        Integer awardId = logicChain.logic(userId, strategyId);
        if (awardId == null) {
            throw new IllegalStateException("抽奖责任链未返回奖品，strategyId: " + strategyId);
        }

        // 检查中奖中规则是否拦截
        StrategyAwardEntity strategyAward = queryStrategyAward(strategyId, awardId);
        if (containsRule(strategyAward.ruleModels(),
                DefaultLogicFactory.LogicModel.RULE_LOCK.getCode())) {
            RuleActionEntity<RuleActionEntity.RaffleCenterEntity> ruleActionCenter =
                    doCheckRaffleCenterLogic(RaffleFactorEntity.builder()
                                    .userId(userId)
                                    .strategyId(strategyId)
                                    .awardId(awardId)
                                    .build(),
                            DefaultLogicFactory.LogicModel.RULE_LOCK.getCode());
            if (ruleActionCenter == null || ruleActionCenter.getCode() == null) {
                throw new IllegalStateException("抽奖中置规则未返回有效结果");
            }
            if (RuleLogicCheckTypeVO.TAKE_OVER.getCode().equals(ruleActionCenter.getCode())) {
                // 中置规则拦截后，后续由抽奖后规则继续兜底处理。
                return RaffleAwardEntity.builder()
                        .strategyId(strategyId)
                        .awardDesc("中奖中规则拦截，通过抽奖后规则 rule_luck_award 走兜底奖励。")
                        .build();
            }
            if (!RuleLogicCheckTypeVO.ALLOW.getCode().equals(ruleActionCenter.getCode())) {
                throw new IllegalStateException(
                        "不支持的抽奖中置规则检查结果，code: " + ruleActionCenter.getCode());
            }
        }

        return RaffleAwardEntity.builder()
                .strategyId(strategyId)
                .awardId(awardId)
                .build();
    }

    /**
     * 查询抽奖奖品配置
     *
     * @param strategyId 策略ID
     * @param awardId    奖品ID
     * @return 抽奖奖品配置
     */
    private StrategyAwardEntity queryStrategyAward(Long strategyId, Integer awardId) {
        List<StrategyAwardEntity> strategyAwards = repository.queryStrategyAwardList(strategyId);
        if (strategyAwards == null || strategyAwards.isEmpty()) {
            throw new IllegalStateException("抽奖奖品配置不存在，strategyId: " + strategyId);
        }
        for (StrategyAwardEntity strategyAward : strategyAwards) {
            if (strategyAward != null && awardId != null && awardId.equals(strategyAward.getAwardId())) {
                return strategyAward;
            }
        }
        throw new IllegalStateException("抽中奖品未配置，strategyId: " + strategyId + ", awardId: " + awardId);
    }


    /**
     * 检查抽奖规则模型是否包含目标规则模型
     *
     * @param ruleModels         抽奖规则模型
     * @param targetRuleModel    目标规则模型
     * @return 是否包含目标规则模型
     */
    private boolean containsRule(String[] ruleModels, String targetRuleModel) {
        if (ruleModels == null) {
            return false;
        }
        for (String ruleModel : ruleModels) {
            if (targetRuleModel.equals(StringUtils.trimToEmpty(ruleModel))) {
                return true;
            }
        }
        return false;
    }

    /** 执行抽奖中置规则。 */
    protected abstract RuleActionEntity<RuleActionEntity.RaffleCenterEntity> doCheckRaffleCenterLogic(
            RaffleFactorEntity raffleFactorEntity, String... ruleModels);
}
