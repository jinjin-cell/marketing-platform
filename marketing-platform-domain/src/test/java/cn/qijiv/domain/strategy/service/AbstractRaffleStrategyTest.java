package cn.qijiv.domain.strategy.service;

import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AbstractRaffleStrategyTest {

    @Test
    public void nonDefaultChainResultStillPassesTreeAndKeepsItsConfig() {
        TestRaffleStrategy strategy = new TestRaffleStrategy(101, 101, "0.01,1", null);

        RaffleAwardEntity result = strategy.performRaffle(factor());

        assertEquals(1, strategy.treeCalls);
        assertEquals(Integer.valueOf(101), result.getAwardId());
        assertEquals("0.01,1", result.getAwardConfig());
    }

    @Test
    public void treeFallbackDoesNotInheritConfigFromDifferentChainAward() {
        TestRaffleStrategy strategy = new TestRaffleStrategy(101, 102, "0.01,1", null);

        RaffleAwardEntity result = strategy.performRaffle(factor());

        assertEquals(1, strategy.treeCalls);
        assertEquals(Integer.valueOf(102), result.getAwardId());
        assertNull(result.getAwardConfig());
    }

    private RaffleFactorEntity factor() {
        return RaffleFactorEntity.builder().userId("unit-user").strategyId(100006L).build();
    }

    private static final class TestRaffleStrategy extends AbstractRaffleStrategy {
        private final Integer chainAwardId;
        private final Integer treeAwardId;
        private final String chainConfig;
        private final String treeConfig;
        private int treeCalls;

        private TestRaffleStrategy(Integer chainAwardId, Integer treeAwardId,
                                   String chainConfig, String treeConfig) {
            super(repository(), null, null);
            this.chainAwardId = chainAwardId;
            this.treeAwardId = treeAwardId;
            this.chainConfig = chainConfig;
            this.treeConfig = treeConfig;
        }

        @Override
        protected DefaultChainFactory.StrategyAwardVO raffleLogicChain(String userId, Long strategyId) {
            return DefaultChainFactory.StrategyAwardVO.builder()
                    .awardId(chainAwardId)
                    .logicModel(DefaultChainFactory.RULE_BLACKLIST)
                    .awardRuleValue(chainConfig)
                    .build();
        }

        @Override
        protected DefaultTreeFactory.StrategyAwardVO raffleLogicTree(
                String userId, Long strategyId, Integer awardId) {
            return raffleLogicTree(userId, strategyId, awardId, null);
        }

        @Override
        protected DefaultTreeFactory.StrategyAwardVO raffleLogicTree(
                String userId, Long strategyId, Integer awardId, java.util.Date endDateTime) {
            treeCalls++;
            return DefaultTreeFactory.StrategyAwardVO.builder()
                    .awardId(treeAwardId)
                    .awardRuleValue(treeConfig)
                    .build();
        }
    }

    private static IStrategyRepository repository() {
        return (IStrategyRepository) Proxy.newProxyInstance(
                IStrategyRepository.class.getClassLoader(),
                new Class<?>[]{IStrategyRepository.class},
                (proxy, method, args) -> {
                    if ("queryStrategyAwardEntity".equals(method.getName())) {
                        Integer awardId = (Integer) args[1];
                        return StrategyAwardEntity.builder()
                                .strategyId((Long) args[0])
                                .awardId(awardId)
                                .awardTitle("award-" + awardId)
                                .sort(awardId)
                                .build();
                    }
                    return null;
                });
    }
}
