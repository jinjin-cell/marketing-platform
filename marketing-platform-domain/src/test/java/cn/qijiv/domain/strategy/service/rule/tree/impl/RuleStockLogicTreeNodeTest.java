package cn.qijiv.domain.strategy.service.rule.tree.impl;

import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.armory.IStrategyDispatch;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RuleStockLogicTreeNodeTest {

    @Test
    public void logic_stockSubtractionSuccess_takesOverAndEnqueuesDatabaseUpdate() {
        AtomicInteger queueCalls = new AtomicInteger();
        DefaultTreeFactory.TreeActionEntity action = new RuleStockLogicTreeNode(
                strategyDispatch(true), repository(queueCalls))
                .logic("user-1", 100001L, 107, "rule-value");

        assertEquals(RuleLogicCheckTypeVO.TAKE_OVER, action.getRuleLogicCheckType());
        assertNotNull(action.getStrategyAwardVO());
        assertEquals(Integer.valueOf(107), action.getStrategyAwardVO().getAwardId());
        assertEquals("rule-value", action.getStrategyAwardVO().getAwardRuleValue());
        assertEquals(1, queueCalls.get());
    }

    @Test
    public void logic_stockSubtractionFailed_allowsFallbackBranchWithoutQueueMessage() {
        AtomicInteger queueCalls = new AtomicInteger();
        DefaultTreeFactory.TreeActionEntity action = new RuleStockLogicTreeNode(
                strategyDispatch(false), repository(queueCalls))
                .logic("user-1", 100001L, 107, null);

        assertEquals(RuleLogicCheckTypeVO.ALLOW, action.getRuleLogicCheckType());
        assertEquals(0, queueCalls.get());
    }

    /** 动态代理只模拟本测试关心的库存调度方法，其他方法不会被节点调用。 */
    private IStrategyDispatch strategyDispatch(boolean subtractionResult) {
        return (IStrategyDispatch) Proxy.newProxyInstance(
                IStrategyDispatch.class.getClassLoader(),
                new Class<?>[]{IStrategyDispatch.class},
                (proxy, method, args) -> {
                    if ("subtractionAwardStock".equals(method.getName())) {
                        return subtractionResult;
                    }
                    throw new UnsupportedOperationException(method.toString());
                });
    }

    private IStrategyRepository repository(AtomicInteger queueCalls) {
        return (IStrategyRepository) Proxy.newProxyInstance(
                IStrategyRepository.class.getClassLoader(),
                new Class<?>[]{IStrategyRepository.class},
                (proxy, method, args) -> {
                    if ("awardStockConsumeSendQueue".equals(method.getName())) {
                        queueCalls.incrementAndGet();
                        return null;
                    }
                    throw new UnsupportedOperationException(method.toString());
                });
    }
}
