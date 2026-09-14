package cn.qijiv.domain.strategy.service.rule.tree.impl;

import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.armory.IStrategyDispatch;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class RuleLuckAwardLogicTreeNodeTest {

    @Test
    public void fallbackAwardReservesStockAndQueuesDatabaseUpdate() {
        AtomicInteger queueCalls = new AtomicInteger();
        AtomicReference<StrategyAwardStockKeyVO> queuedStock = new AtomicReference<>();
        Date endDateTime = new Date();
        RuleLuckAwardLogicTreeNode node = new RuleLuckAwardLogicTreeNode(
                dispatch(true), repository(queueCalls, queuedStock));

        DefaultTreeFactory.TreeActionEntity result = node.logic(
                "unit-user", 100006L, 105, "101:1,100", endDateTime);

        assertEquals(RuleLogicCheckTypeVO.TAKE_OVER, result.getRuleLogicCheckType());
        assertEquals(Integer.valueOf(101), result.getStrategyAwardVO().getAwardId());
        assertEquals("1,100", result.getStrategyAwardVO().getAwardRuleValue());
        assertEquals(1, queueCalls.get());
        assertEquals(Long.valueOf(100006L), queuedStock.get().getStrategyId());
        assertEquals(Integer.valueOf(101), queuedStock.get().getAwardId());
    }

    @Test
    public void exhaustedFallbackAwardDoesNotQueueDatabaseUpdate() {
        AtomicInteger queueCalls = new AtomicInteger();
        RuleLuckAwardLogicTreeNode node = new RuleLuckAwardLogicTreeNode(
                dispatch(false), repository(queueCalls, new AtomicReference<>()));

        AppException exception = assertThrows(AppException.class,
                () -> node.logic("unit-user", 100006L, 105, "101:1,100", new Date()));

        assertEquals(ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getCode(), exception.getCode());
        assertEquals(0, queueCalls.get());
    }

    private IStrategyDispatch dispatch(boolean stockAvailable) {
        return new IStrategyDispatch() {
            @Override
            public Integer getRandomAwardId(Long strategyId) {
                return null;
            }

            @Override
            public Integer getRandomAwardId(Long strategyId, String ruleWeightValue) {
                return null;
            }

            @Override
            public Boolean subtractionAwardStock(Long strategyId, Integer awardId,
                                                 Date endDateTime) {
                assertEquals(Long.valueOf(100006L), strategyId);
                assertEquals(Integer.valueOf(101), awardId);
                return stockAvailable;
            }
        };
    }

    private IStrategyRepository repository(AtomicInteger queueCalls,
                                           AtomicReference<StrategyAwardStockKeyVO> queuedStock) {
        return (IStrategyRepository) Proxy.newProxyInstance(
                IStrategyRepository.class.getClassLoader(),
                new Class<?>[]{IStrategyRepository.class},
                (proxy, method, args) -> {
                    if ("awardStockConsumeSendQueue".equals(method.getName())) {
                        queueCalls.incrementAndGet();
                        queuedStock.set((StrategyAwardStockKeyVO) args[0]);
                    }
                    return null;
                });
    }
}
