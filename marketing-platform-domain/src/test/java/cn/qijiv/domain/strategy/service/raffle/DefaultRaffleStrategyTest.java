package cn.qijiv.domain.strategy.service.raffle;

import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.RuleActionEntity;
import cn.qijiv.domain.strategy.model.entity.RuleMatterEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.armory.IStrategyDispatch;
import cn.qijiv.domain.strategy.service.rule.ILogicFilter;
import cn.qijiv.domain.strategy.service.rule.factory.DefaultLogicFactory;
import cn.qijiv.domain.strategy.service.rule.impl.RuleBackListLogicFilter;
import cn.qijiv.domain.strategy.service.rule.impl.RuleWeightLogicFilter;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultRaffleStrategyTest {

    private static final Long STRATEGY_ID = 100001L;
    private static final String RULE_WEIGHT_VALUE =
            "4000:102,103,104,105 "
                    + "5000:102,103,104,105,106,107 "
                    + "6000:102,103,104,105,106,107,108,109";

    @Test
    public void performRaffle_blacklistTakesPriorityOverWeightRule() {
        StubStrategyRepository repository = new StubStrategyRepository("rule_weight,rule_blacklist");
        RecordingStrategyDispatch strategyDispatch = new RecordingStrategyDispatch();
        DefaultRaffleStrategy raffleStrategy = createRaffleStrategy(repository, strategyDispatch);

        RaffleAwardEntity result = raffleStrategy.performRaffle(RaffleFactorEntity.builder()
                .userId("user001")
                .strategyId(STRATEGY_ID)
                .build());

        assertEquals(Integer.valueOf(101), result.getAwardId());
        assertNull(strategyDispatch.lastRuleWeightValue);
        assertEquals(0, strategyDispatch.defaultRaffleCount);
    }

    @Test
    public void performRaffle_weightRuleUsesMatchedRateTable() {
        StubStrategyRepository repository = new StubStrategyRepository("rule_weight,rule_blacklist");
        RecordingStrategyDispatch strategyDispatch = new RecordingStrategyDispatch();
        DefaultRaffleStrategy raffleStrategy = createRaffleStrategy(repository, strategyDispatch);

        RaffleAwardEntity result = raffleStrategy.performRaffle(RaffleFactorEntity.builder()
                .userId("normal-user")
                .strategyId(STRATEGY_ID)
                .build());

        assertEquals(Integer.valueOf(105), result.getAwardId());
        assertEquals("4000:102,103,104,105", strategyDispatch.lastRuleWeightValue);
        assertEquals(0, strategyDispatch.defaultRaffleCount);
    }

    @Test
    public void performRaffle_withoutRulesUsesDefaultRateTable() {
        StubStrategyRepository repository = new StubStrategyRepository(null);
        RecordingStrategyDispatch strategyDispatch = new RecordingStrategyDispatch();
        DefaultRaffleStrategy raffleStrategy = createRaffleStrategy(repository, strategyDispatch);

        RaffleAwardEntity result = raffleStrategy.performRaffle(RaffleFactorEntity.builder()
                .userId("normal-user")
                .strategyId(STRATEGY_ID)
                .build());

        assertEquals(Integer.valueOf(102), result.getAwardId());
        assertEquals(1, strategyDispatch.defaultRaffleCount);
    }

    @Test
    public void ruleWeight_selectsHighestThresholdNotFirstThreshold() {
        StubStrategyRepository repository = new StubStrategyRepository("rule_weight");
        RuleWeightLogicFilter filter = new RuleWeightLogicFilter(repository) {
            @Override
            protected Long queryUserScore(String userId) {
                return 5500L;
            }
        };
        RuleMatterEntity ruleMatter = new RuleMatterEntity();
        ruleMatter.setUserId("normal-user");
        ruleMatter.setStrategyId(STRATEGY_ID);
        ruleMatter.setRuleModel(DefaultLogicFactory.LogicModel.RULE_WEIGHT.getCode());

        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> result = filter.filter(ruleMatter);

        assertEquals(RuleLogicCheckTypeVO.TAKE_OVER.getCode(), result.getCode());
        assertEquals("5000:102,103,104,105,106,107", result.getData().getRuleWeightValueKey());
    }

    @Test
    public void performRaffle_unknownRuleResultThrows() {
        StubStrategyRepository repository = new StubStrategyRepository(null);
        RecordingStrategyDispatch strategyDispatch = new RecordingStrategyDispatch();
        AbstractRaffleStrategy raffleStrategy = new AbstractRaffleStrategy(repository, strategyDispatch) {
            @Override
            protected RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> doCheckRaffleBeforeLogic(
                    RaffleFactorEntity raffleFactorEntity, String... ruleModels) {
                return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                        .code("9999")
                        .build();
            }

            @Override
            protected RuleActionEntity<RuleActionEntity.RaffleCenterEntity> doCheckRaffleCenterLogic(
                    RaffleFactorEntity raffleFactorEntity, String... ruleModels) {
                return RuleActionEntity.<RuleActionEntity.RaffleCenterEntity>builder()
                        .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                        .build();
            }
        };

        try {
            raffleStrategy.performRaffle(raffleFactor("normal-user"));
            fail("未知规则结果不应继续执行默认抽奖");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("不支持的抽奖规则检查结果"));
            assertEquals(0, strategyDispatch.defaultRaffleCount);
        }
    }

    @Test
    public void performRaffle_blacklistAwardOutsideStrategyThrows() {
        StubStrategyRepository repository =
                new StubStrategyRepository("rule_blacklist", "999:user001");
        DefaultRaffleStrategy raffleStrategy =
                createRaffleStrategy(repository, new RecordingStrategyDispatch());

        try {
            raffleStrategy.performRaffle(raffleFactor("user001"));
            fail("黑名单规则不应返回当前策略不存在的奖品");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("奖品不属于当前策略"));
        }
    }

    private RaffleFactorEntity raffleFactor(String userId) {
        return RaffleFactorEntity.builder()
                .userId(userId)
                .strategyId(STRATEGY_ID)
                .build();
    }

    private DefaultRaffleStrategy createRaffleStrategy(
            StubStrategyRepository repository,
            RecordingStrategyDispatch strategyDispatch) {
        List<ILogicFilter<RuleActionEntity.RaffleBeforeEntity>> filters = Arrays.asList(
                new RuleBackListLogicFilter(repository),
                new RuleWeightLogicFilter(repository)
        );
        return new DefaultRaffleStrategy(
                repository, strategyDispatch, new DefaultLogicFactory(filters));
    }

    private static class StubStrategyRepository implements IStrategyRepository {

        private final String ruleModels;
        private final String blacklistRuleValue;

        private StubStrategyRepository(String ruleModels) {
            this(ruleModels, "101:user001,user002");
        }

        private StubStrategyRepository(String ruleModels, String blacklistRuleValue) {
            this.ruleModels = ruleModels;
            this.blacklistRuleValue = blacklistRuleValue;
        }

        @Override
        public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
            return Arrays.asList(
                    strategyAward(strategyId, 101),
                    strategyAward(strategyId, 102),
                    strategyAward(strategyId, 103),
                    strategyAward(strategyId, 104),
                    strategyAward(strategyId, 105),
                    strategyAward(strategyId, 106),
                    strategyAward(strategyId, 107),
                    strategyAward(strategyId, 108),
                    strategyAward(strategyId, 109));
        }

        private StrategyAwardEntity strategyAward(Long strategyId, Integer awardId) {
            return StrategyAwardEntity.builder()
                    .strategyId(strategyId)
                    .awardId(awardId)
                    .build();
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
            return StrategyEntity.builder()
                    .strategyId(strategyId)
                    .strategyDesc("测试抽奖策略")
                    .ruleModels(ruleModels)
                    .build();
        }

        @Override
        public StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel) {
            if (DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode().equals(ruleModel)) {
                return StrategyRuleEntity.builder()
                        .strategyId(strategyId)
                        .ruleModel(ruleModel)
                        .ruleValue(blacklistRuleValue)
                        .build();
            }
            if (DefaultLogicFactory.LogicModel.RULE_WEIGHT.getCode().equals(ruleModel)) {
                return StrategyRuleEntity.builder()
                        .strategyId(strategyId)
                        .ruleModel(ruleModel)
                        .ruleValue(RULE_WEIGHT_VALUE)
                        .build();
            }
            return null;
        }

        @Override
        public StrategyRuleEntity queryStrategyAwardRule(
                Long strategyId, Integer awardId, String ruleModel) {
            return null;
        }
    }

    private static class RecordingStrategyDispatch implements IStrategyDispatch {

        private int defaultRaffleCount;
        private String lastRuleWeightValue;

        @Override
        public Integer getRandomAwardId(Long strategyId) {
            defaultRaffleCount++;
            return 102;
        }
        @Override
        public Integer getRandomAwardId(Long strategyId, String ruleWeightValue) {
            lastRuleWeightValue = ruleWeightValue;
            return 105;
        }
    }
}
