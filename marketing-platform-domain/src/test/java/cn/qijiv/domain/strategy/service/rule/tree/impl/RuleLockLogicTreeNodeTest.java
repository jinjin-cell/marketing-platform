package cn.qijiv.domain.strategy.service.rule.tree.impl;

import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeVO;
import cn.qijiv.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 验证次数锁规则迁移到规则树节点后仍会拒绝非法配置。 */
public class RuleLockLogicTreeNodeTest {

    @Test
    public void logic_zeroUnlockCount_throws() {
        assertInvalidUnlockCount("0");
    }

    @Test
    public void logic_negativeUnlockCount_throws() {
        assertInvalidUnlockCount("-1");
    }

    private void assertInvalidUnlockCount(String ruleValue) {
        RuleLockLogicTreeNode node = new RuleLockLogicTreeNode(
                new LockRuleRepository(ruleValue));

        try {
            node.logic("test-user", 100001L, 107, null);
            fail("非正数解锁次数不应被接受");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("必须大于0"));
        }
    }

    /** 只返回当前测试所需的奖品次数锁配置。 */
    private static class LockRuleRepository implements IStrategyRepository {

        private final String ruleValue;

        private LockRuleRepository(String ruleValue) {
            this.ruleValue = ruleValue;
        }

        @Override
        public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
            return null;
        }

        @Override
        public void storeStrategyRateTable(String strategyId, List<Integer> rateTable) {
        }

        @Override
        public Integer queryStrategyRateTableSize(String strategyKey) {
            return null;
        }

        @Override
        public Integer queryStrategyAwardId(String strategyKey, Integer randomValue) {
            return null;
        }

        @Override
        public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId) {
            return null;
        }

        @Override
        public StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel) {
            return null;
        }

        @Override
        public StrategyRuleEntity queryStrategyAwardRule(
                Long strategyId, Integer awardId, String ruleModel) {
            return StrategyRuleEntity.builder()
                    .strategyId(strategyId)
                    .awardId(awardId)
                    .ruleModel(ruleModel)
                    .ruleValue(ruleValue)
                    .build();
        }

        @Override
        public StrategyAwardRuleModelVO queryStrategyAwardRuleModelVO(
                Long strategyId, Integer awardId) {
            return null;
        }

        @Override
        public RuleTreeVO queryRuleTreeVOByTreeId(String treeId) {
            return null;
        }

        @Override
        public boolean subtractionAwardStock(Long strategyId, Integer awardId) {
            return true;
        }
    }
}
