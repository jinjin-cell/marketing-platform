package cn.qijiv.domain.strategy.service.rule.tree.impl;

import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;

public class RuleStockLogicTreeNodeTest {

    @Test
    public void logic_stockSubtractionSuccess_allowsOriginalAward() {
        DefaultTreeFactory.TreeActionEntity action = new RuleStockLogicTreeNode(repository(true))
                .logic("user-1", 100001L, 107, null);

        assertEquals(RuleLogicCheckTypeVO.ALLOW, action.getRuleLogicCheckType());
    }

    @Test
    public void logic_stockSubtractionFailed_routesToTakeOverBranch() {
        DefaultTreeFactory.TreeActionEntity action = new RuleStockLogicTreeNode(repository(false))
                .logic("user-1", 100001L, 107, null);

        assertEquals(RuleLogicCheckTypeVO.TAKE_OVER, action.getRuleLogicCheckType());
    }

    /** 动态代理只模拟本测试关心的库存仓储方法，其他方法不会被节点调用。 */
    private IStrategyRepository repository(boolean subtractionResult) {
        return (IStrategyRepository) Proxy.newProxyInstance(
                IStrategyRepository.class.getClassLoader(),
                new Class<?>[]{IStrategyRepository.class},
                (proxy, method, args) -> {
                    if ("subtractionAwardStock".equals(method.getName())) {
                        return subtractionResult;
                    }
                    throw new UnsupportedOperationException(method.toString());
                });
    }
}
