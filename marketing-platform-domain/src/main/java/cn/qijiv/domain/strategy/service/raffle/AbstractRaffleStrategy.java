package cn.qijiv.domain.strategy.service.raffle;

import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.RuleActionEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.IRaffleStrategy;
import cn.qijiv.domain.strategy.service.armory.IStrategyDispatch;
import cn.qijiv.domain.strategy.service.rule.factory.DefaultLogicFactory;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import org.apache.commons.lang3.StringUtils;

/**
 * 抽奖策略抽象类
 *
 * @author jinlujia
 * @date 2026/07/18
 */
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {

    protected final IStrategyRepository repository;
    protected final IStrategyDispatch strategyDispatch;

    protected AbstractRaffleStrategy(IStrategyRepository repository, IStrategyDispatch strategyDispatch) {
        this.repository = repository;
        this.strategyDispatch = strategyDispatch;
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

        Long strategyId = raffleFactorEntity.getStrategyId();
        StrategyEntity strategy = repository.queryStrategyEntityByStrategyId(strategyId);
        if (strategy == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "抽奖策略不存在");
        }

        // 模板方法先执行抽奖前规则；子类只负责规则的具体编排方式。
        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleAction =
                doCheckRaffleBeforeLogic(raffleFactorEntity, strategy.ruleModels());

        if (ruleAction == null || ruleAction.getCode() == null) {
            throw new IllegalStateException("抽奖前置规则未返回有效结果");
        }

        Integer awardId;
        if (RuleLogicCheckTypeVO.TAKE_OVER.getCode().equals(ruleAction.getCode())) {
            // TAKE_OVER 表示规则已经改变了正常抽奖路径，需要按规则结果选奖。
            awardId = getTakeOverAwardId(strategyId, ruleAction);
        } else if (RuleLogicCheckTypeVO.ALLOW.getCode().equals(ruleAction.getCode())) {
            // 所有前置规则放行时，使用策略的完整概率表抽奖。
            awardId = strategyDispatch.getRandomAwardId(strategyId);
        } else {
            throw new IllegalStateException("不支持的抽奖规则检查结果，code: " + ruleAction.getCode());
        }

        
        return RaffleAwardEntity.builder()
                .strategyId(strategyId)
                .awardId(awardId)
                .build();
    }

    /**
     * 获取接管抽奖的奖品ID
     *
     * @param strategyId 策略ID
     * @param ruleAction 规则检查结果
     * @return 取管抽奖的奖品ID
     */
    private Integer getTakeOverAwardId(
            Long strategyId,
            RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleAction) {
        RuleActionEntity.RaffleBeforeEntity data = ruleAction.getData();
        if (data == null) {
            throw new IllegalStateException("规则接管抽奖时必须返回规则数据");
        }

        if (DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode().equals(ruleAction.getRuleModel())) {
            if (data.getAwardId() == null) {
                throw new IllegalStateException("黑名单规则未返回指定奖品");
            }
            // 黑名单不参与随机计算，直接返回规则中配置的固定奖品。
            return data.getAwardId();
        }

        if (DefaultLogicFactory.LogicModel.RULE_WEIGHT.getCode().equals(ruleAction.getRuleModel())) {
            // 权重规则只缩小可抽范围，最终仍通过对应档位的概率表随机选奖。
            return strategyDispatch.getRandomAwardId(strategyId, data.getRuleWeightValueKey());
        }

        throw new IllegalStateException("不支持的接管规则，ruleModel: " + ruleAction.getRuleModel());
    }

    /** 执行抽奖前置规则。 */
    protected abstract RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> doCheckRaffleBeforeLogic(
            RaffleFactorEntity raffleFactorEntity, String... ruleModels);
}
