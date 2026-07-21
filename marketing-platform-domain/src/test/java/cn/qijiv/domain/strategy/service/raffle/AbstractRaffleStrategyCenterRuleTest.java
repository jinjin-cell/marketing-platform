package cn.qijiv.domain.strategy.service.raffle;

import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.RuleActionEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.armory.IStrategyDispatch;
import cn.qijiv.domain.strategy.service.rule.factory.DefaultLogicFactory;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AbstractRaffleStrategyCenterRuleTest {

    private static final Long STRATEGY_ID = 100001L;
    private static final Integer AWARD_ID = 107;

    @Test
    public void performRaffle_centerRuleAllows_returnsSelectedAward() {
        AbstractRaffleStrategy strategy = strategyWithCenterResult(action(RuleLogicCheckTypeVO.ALLOW.getCode()));

        RaffleAwardEntity result = strategy.performRaffle(raffleFactor());

        assertEquals(STRATEGY_ID, result.getStrategyId());
        assertEquals(AWARD_ID, result.getAwardId());
    }

    @Test
    public void performRaffle_centerRuleTakesOver_doesNotReturnLockedAward() {
        AbstractRaffleStrategy strategy = strategyWithCenterResult(action(RuleLogicCheckTypeVO.TAKE_OVER.getCode()));

        RaffleAwardEntity result = strategy.performRaffle(raffleFactor());

        assertEquals(STRATEGY_ID, result.getStrategyId());
        assertNull(result.getAwardId());
        assertTrue(result.getAwardDesc().contains("rule_luck_award"));
    }

    @Test
    public void performRaffle_centerRuleReturnsNull_throws() {
        assertInvalidCenterResult(null, "抽奖中置规则未返回有效结果");
    }

    @Test
    public void performRaffle_centerRuleReturnsNullCode_throws() {
        assertInvalidCenterResult(action(null), "抽奖中置规则未返回有效结果");
    }

    @Test
    public void performRaffle_centerRuleReturnsUnknownCode_throws() {
        assertInvalidCenterResult(action("9999"), "不支持的抽奖中置规则检查结果");
    }

    private void assertInvalidCenterResult(
            RuleActionEntity<RuleActionEntity.RaffleCenterEntity> centerResult,
            String expectedMessage) {
        try {
            strategyWithCenterResult(centerResult).performRaffle(raffleFactor());
            fail("非法中置规则结果不应继续发奖");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains(expectedMessage));
        }
    }

    private AbstractRaffleStrategy strategyWithCenterResult(
            RuleActionEntity<RuleActionEntity.RaffleCenterEntity> centerResult) {
        return new AbstractRaffleStrategy(new CenterRuleRepository(), new FixedStrategyDispatch()) {
            @Override
            protected RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> doCheckRaffleBeforeLogic(
                    RaffleFactorEntity raffleFactorEntity, String... ruleModels) {
                return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                        .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                        .build();
            }

            @Override
            protected RuleActionEntity<RuleActionEntity.RaffleCenterEntity> doCheckRaffleCenterLogic(
                    RaffleFactorEntity raffleFactorEntity, String... ruleModels) {
                return centerResult;
            }
        };
    }

    private RuleActionEntity<RuleActionEntity.RaffleCenterEntity> action(String code) {
        return RuleActionEntity.<RuleActionEntity.RaffleCenterEntity>builder()
                .ruleModel(DefaultLogicFactory.LogicModel.RULE_LOCK.getCode())
                .code(code)
                .build();
    }

    private RaffleFactorEntity raffleFactor() {
        return RaffleFactorEntity.builder()
                .userId("test-user")
                .strategyId(STRATEGY_ID)
                .build();
    }

    private static class CenterRuleRepository implements IStrategyRepository {

        @Override
        public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
            return Collections.singletonList(StrategyAwardEntity.builder()
                    .strategyId(strategyId)
                    .awardId(AWARD_ID)
                    .ruleModels(DefaultLogicFactory.LogicModel.RULE_LOCK.getCode())
                    .build());
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
            return StrategyEntity.builder().strategyId(strategyId).build();
        }

        @Override
        public StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel) {
            return null;
        }

        @Override
        public StrategyRuleEntity queryStrategyAwardRule(
                Long strategyId, Integer awardId, String ruleModel) {
            return null;
        }
    }

    private static class FixedStrategyDispatch implements IStrategyDispatch {

        @Override
        public Integer getRandomAwardId(Long strategyId) {
            return AWARD_ID;
        }

        @Override
        public Integer getRandomAwardId(Long strategyId, String ruleWeightValue) {
            return AWARD_ID;
        }
    }
}
