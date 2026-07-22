package cn.qijiv.domain.strategy.service.raffle;

import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.RuleActionEntity;
import cn.qijiv.domain.strategy.model.entity.RuleMatterEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.rule.ILogicFilter;
import cn.qijiv.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.qijiv.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/** 默认抽奖策略。 */
@Service
public class DefaultRaffleStrategy extends AbstractRaffleStrategy {

    private final DefaultLogicFactory logicFactory;

    public DefaultRaffleStrategy(
            IStrategyRepository repository,
            DefaultChainFactory chainFactory,
            DefaultLogicFactory logicFactory) {
        super(repository, chainFactory);
        this.logicFactory = logicFactory;
    }

    /**
     * 执行抽奖中逻辑检查
     *
     * @param raffleFactorEntity 抽奖因子
     * @param ruleModels         抽奖规则模型
     * @return 抽奖中逻辑检查结果
     */
    @Override
    protected RuleActionEntity<RuleActionEntity.RaffleCenterEntity> doCheckRaffleCenterLogic(
            RaffleFactorEntity raffleFactorEntity, String... ruleModels) {
        Map<String, ILogicFilter<RuleActionEntity.RaffleCenterEntity>> logicFilters =
                logicFactory.openCenterLogicFilter();

        if (ruleModels != null) {
            for (String configuredRuleModel : ruleModels) {
                String ruleModel = StringUtils.trimToEmpty(configuredRuleModel);
                if (ruleModel.isEmpty()) {
                    continue;
                }
                RuleActionEntity<RuleActionEntity.RaffleCenterEntity> ruleAction =
                        executeCenterRule(logicFilters, raffleFactorEntity, ruleModel);
                if (!RuleLogicCheckTypeVO.ALLOW.getCode().equals(ruleAction.getCode())) {
                    return ruleAction;
                }
            }
        }

        return RuleActionEntity.<RuleActionEntity.RaffleCenterEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }

    /**
     * 执行抽奖中置逻辑检查
     *
     * @param logicFilters   抽奖中置逻辑检查过滤器
     * @param raffleFactorEntity 抽奖因子
     * @param ruleModel        抽奖规则模型
     * @return 抽奖中置逻辑检查结果
     */
    private RuleActionEntity<RuleActionEntity.RaffleCenterEntity> executeCenterRule(
            Map<String, ILogicFilter<RuleActionEntity.RaffleCenterEntity>> logicFilters,
            RaffleFactorEntity raffleFactorEntity,
            String ruleModel) {
        ILogicFilter<RuleActionEntity.RaffleCenterEntity> logicFilter = logicFilters.get(ruleModel);
        if (logicFilter == null) {
            throw new IllegalStateException("抽奖中置规则过滤器未注册，ruleModel: " + ruleModel);
        }

        RuleMatterEntity ruleMatterEntity = new RuleMatterEntity();
        ruleMatterEntity.setUserId(raffleFactorEntity.getUserId());
        ruleMatterEntity.setStrategyId(raffleFactorEntity.getStrategyId());
        ruleMatterEntity.setAwardId(raffleFactorEntity.getAwardId());
        ruleMatterEntity.setRuleModel(ruleModel);

        RuleActionEntity<RuleActionEntity.RaffleCenterEntity> ruleAction =
                logicFilter.filter(ruleMatterEntity);
        if (ruleAction == null || ruleAction.getCode() == null) {
            throw new IllegalStateException("抽奖中置规则过滤器返回结果为空，ruleModel: " + ruleModel);
        }
        return ruleAction;
    }

}
