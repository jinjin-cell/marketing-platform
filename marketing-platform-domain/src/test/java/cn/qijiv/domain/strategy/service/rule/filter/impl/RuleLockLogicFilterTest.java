package cn.qijiv.domain.strategy.service.rule.filter.impl;

import cn.qijiv.domain.strategy.model.entity.RuleMatterEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RuleLockLogicFilterTest {

    @Test
    public void filter_zeroUnlockCount_throws() {
        assertInvalidUnlockCount("0");
    }

    @Test
    public void filter_negativeUnlockCount_throws() {
        assertInvalidUnlockCount("-1");
    }

    private void assertInvalidUnlockCount(String ruleValue) {
        RuleLockLogicFilter filter = new RuleLockLogicFilter(new LockRuleRepository(ruleValue));

        try {
            filter.filter(ruleMatter());
            fail("非正数解锁次数不应被接受");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("必须大于0"));
        }
    }

    private RuleMatterEntity ruleMatter() {
        RuleMatterEntity ruleMatter = new RuleMatterEntity();
        ruleMatter.setUserId("test-user");
        ruleMatter.setStrategyId(100001L);
        ruleMatter.setAwardId(107);
        ruleMatter.setRuleModel(DefaultLogicFactory.LogicModel.RULE_LOCK.getCode());
        return ruleMatter;
    }

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
    }
}
