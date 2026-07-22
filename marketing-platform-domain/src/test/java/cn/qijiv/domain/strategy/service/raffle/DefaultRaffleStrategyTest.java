package cn.qijiv.domain.strategy.service.raffle;

import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.armory.IStrategyDispatch;
import cn.qijiv.domain.strategy.service.rule.chain.ILogicChain;
import cn.qijiv.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.qijiv.domain.strategy.service.rule.chain.impl.BackListLogicChain;
import cn.qijiv.domain.strategy.service.rule.chain.impl.DefaultLogicChain;
import cn.qijiv.domain.strategy.service.rule.chain.impl.RuleWeightLogicChain;
import cn.qijiv.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import org.junit.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
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
    public void performRaffle_blacklistTakesPriorityOverConfiguredWeightRule() {
        StubStrategyRepository repository = new StubStrategyRepository("rule_weight,rule_blacklist");
        RecordingStrategyDispatch dispatch = new RecordingStrategyDispatch();

        RaffleAwardEntity result = createRaffleStrategy(repository, dispatch)
                .performRaffle(raffleFactor("user001"));

        assertEquals(Integer.valueOf(101), result.getAwardId());
        assertNull(dispatch.lastRuleWeightValue);
        assertEquals(0, dispatch.defaultRaffleCount);
    }

    @Test
    public void performRaffle_weightRuleUsesMatchedRateTable() {
        StubStrategyRepository repository = new StubStrategyRepository("rule_weight,rule_blacklist");
        RecordingStrategyDispatch dispatch = new RecordingStrategyDispatch();

        RaffleAwardEntity result = createRaffleStrategy(repository, dispatch)
                .performRaffle(raffleFactor("normal-user"));

        assertEquals(Integer.valueOf(105), result.getAwardId());
        assertEquals("4000:102,103,104,105", dispatch.lastRuleWeightValue);
        assertEquals(0, dispatch.defaultRaffleCount);
    }

    @Test
    public void performRaffle_withoutRulesUsesDefaultNode() {
        StubStrategyRepository repository = new StubStrategyRepository(null);
        RecordingStrategyDispatch dispatch = new RecordingStrategyDispatch();

        RaffleAwardEntity result = createRaffleStrategy(repository, dispatch)
                .performRaffle(raffleFactor("normal-user"));

        assertEquals(Integer.valueOf(102), result.getAwardId());
        assertEquals(1, dispatch.defaultRaffleCount);
    }

    @Test
    public void weightNode_selectsHighestSatisfiedThreshold() {
        StubStrategyRepository repository = new StubStrategyRepository("rule_weight");
        RecordingStrategyDispatch dispatch = new RecordingStrategyDispatch();
        RuleWeightLogicChain weightChain = new RuleWeightLogicChain(repository, dispatch) {
            @Override
            protected Long queryUserScore(String userId) {
                return 5500L;
            }
        };
        weightChain.appendNext(new DefaultLogicChain(dispatch));

        Integer awardId = weightChain.logic("normal-user", STRATEGY_ID);

        assertEquals(Integer.valueOf(105), awardId);
        assertEquals("5000:102,103,104,105,106,107", dispatch.lastRuleWeightValue);
    }

    @Test
    public void performRaffle_unknownRuleModelThrows() {
        StubStrategyRepository repository = new StubStrategyRepository("rule_unknown");

        try {
            createRaffleStrategy(repository, new RecordingStrategyDispatch())
                    .performRaffle(raffleFactor("normal-user"));
            fail("未注册的责任链节点不应被忽略");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("责任链节点未注册"));
        }
    }

    @Test
    public void performRaffle_blacklistAwardOutsideStrategyThrows() {
        StubStrategyRepository repository =
                new StubStrategyRepository("rule_blacklist", "999:user001");

        try {
            createRaffleStrategy(repository, new RecordingStrategyDispatch())
                    .performRaffle(raffleFactor("user001"));
            fail("黑名单规则不应返回当前策略不存在的奖品");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("奖品不属于当前策略"));
        }
    }

    private DefaultRaffleStrategy createRaffleStrategy(
            StubStrategyRepository repository,
            RecordingStrategyDispatch dispatch) {
        DefaultChainFactory chainFactory = new DefaultChainFactory(
                chainBeanFactory(repository, dispatch), repository);
        return new DefaultRaffleStrategy(
                repository,
                chainFactory,
                new DefaultLogicFactory(Collections.emptyList()));
    }

    private ListableBeanFactory chainBeanFactory(
            StubStrategyRepository repository,
            RecordingStrategyDispatch dispatch) {
        return (ListableBeanFactory) Proxy.newProxyInstance(
                ListableBeanFactory.class.getClassLoader(),
                new Class<?>[]{ListableBeanFactory.class},
                (proxy, method, args) -> {
                    if ("getBean".equals(method.getName())
                            && args != null
                            && args.length == 2
                            && args[0] instanceof String) {
                        String beanName = (String) args[0];
                        if ("rule_blacklist".equals(beanName)) {
                            return new BackListLogicChain(repository);
                        }
                        if ("rule_weight".equals(beanName)) {
                            return new RuleWeightLogicChain(repository, dispatch);
                        }
                        if ("default".equals(beanName)) {
                            return new DefaultLogicChain(dispatch);
                        }
                        throw new NoSuchBeanDefinitionException(beanName);
                    }
                    if ("toString".equals(method.getName())) {
                        return "TestChainBeanFactory";
                    }
                    throw new UnsupportedOperationException(method.toString());
                });
    }

    private RaffleFactorEntity raffleFactor(String userId) {
        return RaffleFactorEntity.builder()
                .userId(userId)
                .strategyId(STRATEGY_ID)
                .build();
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
            if ("rule_blacklist".equals(ruleModel)) {
                return StrategyRuleEntity.builder()
                        .strategyId(strategyId)
                        .ruleModel(ruleModel)
                        .ruleValue(blacklistRuleValue)
                        .build();
            }
            if ("rule_weight".equals(ruleModel)) {
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
