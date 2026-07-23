package cn.qijiv.domain.strategy.service.rule.tree.factory.engine.impl;

import cn.qijiv.domain.strategy.model.valobj.RuleLimitTypeVO;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeNodeLineVO;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeNodeVO;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeVO;
import cn.qijiv.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DecisionTreeEngineTest {

    @Test
    public void process_lockAndStockAllow_finishesWithoutTakeOver() {
        List<String> visited = new ArrayList<>();
        Map<String, ILogicTreeNode> nodes = new LinkedHashMap<>();
        nodes.put("rule_lock", node("lock", RuleLogicCheckTypeVO.ALLOW, visited));
        nodes.put("rule_stock", node("stock", RuleLogicCheckTypeVO.ALLOW, visited));
        nodes.put("rule_luck_award", luckNode(visited));

        DefaultTreeFactory.StrategyAwardVO result = engine(nodes, standardTree())
                .process("user-1", 100001L, 107);

        assertNull(result);
        assertEquals(Arrays.asList("lock", "stock"), visited);
    }

    @Test
    public void process_lockTakesOver_routesToLuckAward() {
        List<String> visited = new ArrayList<>();
        Map<String, ILogicTreeNode> nodes = new LinkedHashMap<>();
        nodes.put("rule_lock", node("lock", RuleLogicCheckTypeVO.TAKE_OVER, visited));
        nodes.put("rule_stock", node("stock", RuleLogicCheckTypeVO.ALLOW, visited));
        nodes.put("rule_luck_award", luckNode(visited));

        DefaultTreeFactory.StrategyAwardVO result = engine(nodes, standardTree())
                .process("user-1", 100001L, 107);

        assertEquals(Integer.valueOf(101), result.getAwardId());
        assertEquals("1,100", result.getAwardRuleValue());
        assertEquals(Arrays.asList("lock", "luck"), visited);
    }

    @Test
    public void process_stockTakesOver_routesToLuckAward() {
        List<String> visited = new ArrayList<>();
        Map<String, ILogicTreeNode> nodes = new LinkedHashMap<>();
        nodes.put("rule_lock", node("lock", RuleLogicCheckTypeVO.ALLOW, visited));
        nodes.put("rule_stock", node("stock", RuleLogicCheckTypeVO.TAKE_OVER, visited));
        nodes.put("rule_luck_award", luckNode(visited));

        DefaultTreeFactory.StrategyAwardVO result = engine(nodes, standardTree())
                .process("user-1", 100001L, 107);

        assertEquals(Integer.valueOf(101), result.getAwardId());
        assertEquals(Arrays.asList("lock", "stock", "luck"), visited);
    }

    @Test
    public void process_missingNodeImplementation_throws() {
        try {
            engine(Collections.emptyMap(), standardTree())
                    .process("user-1", 100001L, 107);
            fail("未注册的规则树节点不应被忽略");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("节点实现未注册"));
        }
    }

    @Test
    public void process_takeOverWithoutMatchingBranchOrAward_throws() {
        Map<String, ILogicTreeNode> nodes = new LinkedHashMap<>();
        nodes.put("rule_lock", (userId, strategyId, awardId, ruleValue) ->
                action(RuleLogicCheckTypeVO.TAKE_OVER, null));

        RuleTreeNodeVO lock = RuleTreeNodeVO.builder()
                .treeId("tree_lock")
                .ruleKey("rule_lock")
                .treeNodeLineVOList(Collections.singletonList(
                        line("lock", "unused", RuleLogicCheckTypeVO.ALLOW)))
                .build();
        Map<String, RuleTreeNodeVO> nodeMap = new LinkedHashMap<>();
        nodeMap.put("lock", lock);

        try {
            engine(nodes, tree("lock", nodeMap)).process("user-1", 100001L, 107);
            fail("规则接管但没有后续分支或奖品时不应放行原奖品");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("接管结果缺少后续分支或奖品"));
        }
    }

    @Test
    public void process_cyclicTree_throws() {
        Map<String, ILogicTreeNode> nodes = new LinkedHashMap<>();
        nodes.put("rule_lock", (userId, strategyId, awardId, ruleValue) ->
                action(RuleLogicCheckTypeVO.ALLOW, null));

        RuleTreeNodeVO lock = RuleTreeNodeVO.builder()
                .treeId("tree_lock")
                .ruleKey("rule_lock")
                .treeNodeLineVOList(Collections.singletonList(
                        line("lock", "lock", RuleLogicCheckTypeVO.ALLOW)))
                .build();
        Map<String, RuleTreeNodeVO> nodeMap = new LinkedHashMap<>();
        nodeMap.put("lock", lock);

        try {
            engine(nodes, tree("lock", nodeMap)).process("user-1", 100001L, 107);
            fail("存在环路的规则树不应继续执行");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("存在环路"));
        }
    }

    private DecisionTreeEngine engine(Map<String, ILogicTreeNode> nodes, RuleTreeVO tree) {
        return new DecisionTreeEngine(nodes, tree);
    }

    private ILogicTreeNode node(
            String name, RuleLogicCheckTypeVO checkType, List<String> visited) {
        return (userId, strategyId, awardId, ruleValue) -> {
            visited.add(name);
            return action(checkType, null);
        };
    }

    private ILogicTreeNode luckNode(List<String> visited) {
        return (userId, strategyId, awardId, ruleValue) -> {
            visited.add("luck");
            return action(
                    RuleLogicCheckTypeVO.TAKE_OVER,
                    DefaultTreeFactory.StrategyAwardVO.builder()
                            .awardId(101)
                            .awardRuleValue("1,100")
                            .build());
        };
    }

    private DefaultTreeFactory.TreeActionEntity action(
            RuleLogicCheckTypeVO checkType,
            DefaultTreeFactory.StrategyAwardVO data) {
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(checkType)
                .strategyAwardVO(data)
                .build();
    }

    private RuleTreeVO standardTree() {
        RuleTreeNodeVO lock = RuleTreeNodeVO.builder()
                .treeId("tree_lock")
                .ruleKey("rule_lock")
                .treeNodeLineVOList(Arrays.asList(
                        line("lock", "stock", RuleLogicCheckTypeVO.ALLOW),
                        line("lock", "luck", RuleLogicCheckTypeVO.TAKE_OVER)))
                .build();
        RuleTreeNodeVO stock = RuleTreeNodeVO.builder()
                .treeId("tree_lock")
                .ruleKey("rule_stock")
                .treeNodeLineVOList(Collections.singletonList(
                        line("stock", "luck", RuleLogicCheckTypeVO.TAKE_OVER)))
                .build();
        RuleTreeNodeVO luck = RuleTreeNodeVO.builder()
                .treeId("tree_lock")
                .ruleKey("rule_luck_award")
                .ruleValue("1,100")
                .treeNodeLineVOList(Collections.emptyList())
                .build();

        Map<String, RuleTreeNodeVO> nodeMap = new LinkedHashMap<>();
        nodeMap.put("lock", lock);
        nodeMap.put("stock", stock);
        nodeMap.put("luck", luck);
        return tree("lock", nodeMap);
    }

    private RuleTreeVO tree(String root, Map<String, RuleTreeNodeVO> nodeMap) {
        return RuleTreeVO.builder()
                .treeId("tree_lock")
                .treeName("抽奖后置规则树")
                .treeRootRuleNode(root)
                .treeNodeMap(nodeMap)
                .build();
    }

    private RuleTreeNodeLineVO line(
            String from, String to, RuleLogicCheckTypeVO expected) {
        return RuleTreeNodeLineVO.builder()
                .treeId("tree_lock")
                .ruleNodeFrom(from)
                .ruleNodeTo(to)
                .ruleLimitType(RuleLimitTypeVO.EQUAL)
                .ruleLimitValue(expected)
                .build();
    }
}
