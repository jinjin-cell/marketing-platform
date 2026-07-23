package cn.qijiv.domain.strategy.service.rule.tree.impl;

import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import org.springframework.stereotype.Component;

/** 规则树中的库存校验和扣减节点。 */
@Component("rule_stock")
public class RuleStockLogicTreeNode implements ILogicTreeNode {

    private final IStrategyRepository repository;

    public RuleStockLogicTreeNode(IStrategyRepository repository) {
        this.repository = repository;
    }

    @Override
    public DefaultTreeFactory.TreeActionEntity logic(
            String userId, Long strategyId, Integer awardId, String ruleValue) {
        // 仓储使用“库存 > 0 才更新”的单条 SQL，把库存判断和扣减合并为原子操作。
        boolean subtractionSuccess = repository.subtractionAwardStock(strategyId, awardId);
        // 扣减失败表示库存不足，由 TAKE_OVER 分支转入兜底奖励节点。
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(subtractionSuccess
                        ? RuleLogicCheckTypeVO.ALLOW
                        : RuleLogicCheckTypeVO.TAKE_OVER)
                .build();
    }
}
