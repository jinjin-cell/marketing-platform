package cn.qijiv.domain.strategy.service.raffle;

import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeVO;
import cn.qijiv.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.qijiv.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.AbstractRaffleStrategy;
import cn.qijiv.domain.strategy.service.IRaffleAward;
import cn.qijiv.domain.strategy.service.IRaffleStock;
import cn.qijiv.domain.strategy.service.rule.chain.ILogicChain;
import cn.qijiv.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.qijiv.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/** 默认抽奖策略，负责实现模板方法中两个可变的业务步骤。 */
@Service
public class DefaultRaffleStrategy extends AbstractRaffleStrategy implements IRaffleAward, IRaffleStock {

    public DefaultRaffleStrategy(
            IStrategyRepository repository,
            DefaultChainFactory defaultChainFactory,
            DefaultTreeFactory defaultTreeFactory) {
        super(repository, defaultChainFactory, defaultTreeFactory);
    }

    /** 打开当前策略对应的责任链并执行抽奖。 */
    @Override
    protected DefaultChainFactory.StrategyAwardVO raffleLogicChain(
            String userId, Long strategyId) {
        ILogicChain logicChain = defaultChainFactory.openLogicChain(strategyId);
        return logicChain.logic(userId, strategyId);
    }

    /**
     * 查询奖品绑定的规则树并执行。
     *
     * <p>奖品没有绑定规则树时，原奖品直接通过；绑定后则从数据库装配完整规则树，
     * 再交给决策树引擎逐节点执行。</p>
     */
    @Override
    protected DefaultTreeFactory.StrategyAwardVO raffleLogicTree(
            String userId, Long strategyId, Integer awardId) {
        StrategyAwardRuleModelVO strategyAwardRuleModelVO =
                repository.queryStrategyAwardRuleModelVO(strategyId, awardId);
        if (strategyAwardRuleModelVO == null) {
            throw new IllegalStateException(
                    "抽中奖品不存在，strategyId: " + strategyId + ", awardId: " + awardId);
        }
        if (StringUtils.isBlank(strategyAwardRuleModelVO.getRuleModels())) {
            // 没有后置规则的奖品不需要创建规则树，保留责任链抽中的原奖品。
            return originalAward(awardId);
        }

        String treeId = strategyAwardRuleModelVO.getRuleModels().trim();
        RuleTreeVO ruleTree = repository.queryRuleTreeVOByTreeId(treeId);
        if (ruleTree == null) {
            throw new IllegalStateException(
                    "奖品绑定的规则树不存在，strategyId: " + strategyId
                            + ", awardId: " + awardId + ", treeId: " + treeId);
        }

        IDecisionTreeEngine treeEngine = defaultTreeFactory.openLogicTree(ruleTree);
        DefaultTreeFactory.StrategyAwardVO result =
                treeEngine.process(userId, strategyId, awardId);
        // 所有规则均放行时引擎不会接管结果，继续发放责任链抽中的原奖品。
        return result == null ? originalAward(awardId) : result;
    }

    private DefaultTreeFactory.StrategyAwardVO originalAward(Integer awardId) {
        return DefaultTreeFactory.StrategyAwardVO.builder()
                .awardId(awardId)
                .build();
    }

     @Override
    public StrategyAwardStockKeyVO takeQueueValue() throws InterruptedException {
        return repository.takeQueueValue();
    }

    @Override
    public void updateStrategyAwardStock(Long strategyId, Integer awardId) {
        repository.updateStrategyAwardStock(strategyId, awardId);
    }

    @Override
    public List<StrategyAwardEntity> queryRaffleStrategyAwardList(Long strategyId) {
        return repository.queryStrategyAwardList(strategyId);
    }

}
