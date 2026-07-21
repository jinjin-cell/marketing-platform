package cn.qijiv.domain.strategy.service.raffle;

import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.RuleActionEntity;
import cn.qijiv.domain.strategy.model.entity.RuleMatterEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.armory.IStrategyDispatch;
import cn.qijiv.domain.strategy.service.rule.ILogicFilter;
import cn.qijiv.domain.strategy.service.rule.factory.DefaultLogicFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/** 默认抽奖策略。 */
@Service
public class DefaultRaffleStrategy extends AbstractRaffleStrategy {

    private final DefaultLogicFactory logicFactory;

    public DefaultRaffleStrategy(
            IStrategyRepository repository,
            IStrategyDispatch strategyDispatch,
            DefaultLogicFactory logicFactory) {
        super(repository, strategyDispatch);
        this.logicFactory = logicFactory;
    }

    /**
     * 执行抽奖前逻辑检查
     *
     * @param raffleFactorEntity 抽奖因子
     * @param ruleModels         抽奖规则模型
     * @return 抽奖前逻辑检查结果
     */
    @Override
    protected RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> doCheckRaffleBeforeLogic(
            RaffleFactorEntity raffleFactorEntity, String... ruleModels) {
        // 打开逻辑过滤器
        Map<String, ILogicFilter<RuleActionEntity.RaffleBeforeEntity>> logicFilters =
                logicFactory.openLogicFilter();

        // 黑名单拥有最高优先级，即使配置顺序靠后也必须先执行。
        String blacklistModel = DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode();
        if (containsRule(ruleModels, blacklistModel)) {
            RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> blacklistAction =
                    executeRule(logicFilters, raffleFactorEntity, blacklistModel);
            if (!RuleLogicCheckTypeVO.ALLOW.getCode().equals(blacklistAction.getCode())) {
                return blacklistAction;
            }
        }

        // 黑名单放行后，其余规则保持策略配置顺序执行，首个非放行结果终止规则链。
        if (ruleModels != null) {
            for (String configuredRuleModel : ruleModels) {
                String ruleModel = StringUtils.trimToEmpty(configuredRuleModel);
                if (ruleModel.isEmpty() || blacklistModel.equals(ruleModel)) {
                    continue;
                }
                RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleAction =
                        executeRule(logicFilters, raffleFactorEntity, ruleModel);
                if (!RuleLogicCheckTypeVO.ALLOW.getCode().equals(ruleAction.getCode())) {
                    return ruleAction;
                }
            }
        }

        return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }

    /**
     * 执行抽奖前逻辑检查
     *
     * @param logicFilters   抽奖前逻辑检查过滤器
     * @param raffleFactorEntity 抽奖因子
     * @param ruleModel        抽奖规则模型
     * @return 抽奖前逻辑检查结果
     */
    private RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> executeRule(
            Map<String, ILogicFilter<RuleActionEntity.RaffleBeforeEntity>> logicFilters,
            RaffleFactorEntity raffleFactorEntity,
            String ruleModel) {
        ILogicFilter<RuleActionEntity.RaffleBeforeEntity> logicFilter = logicFilters.get(ruleModel);
        if (logicFilter == null) {
            throw new IllegalStateException("抽奖规则过滤器未注册，ruleModel: " + ruleModel);
        }

        // 将统一的抽奖因子转换为每个过滤器都能消费的规则物料。
        RuleMatterEntity ruleMatterEntity = new RuleMatterEntity();
        ruleMatterEntity.setUserId(raffleFactorEntity.getUserId());
        ruleMatterEntity.setStrategyId(raffleFactorEntity.getStrategyId());
        ruleMatterEntity.setRuleModel(ruleModel);

        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleAction =
                logicFilter.filter(ruleMatterEntity);
        if (ruleAction == null || ruleAction.getCode() == null) {
            throw new IllegalStateException("抽奖规则过滤器返回结果为空，ruleModel: " + ruleModel);
        }
        return ruleAction;
    }

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
}
