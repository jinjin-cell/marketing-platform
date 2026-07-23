package cn.qijiv.domain.strategy.service.rule.tree.factory.engine.impl;

import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeNodeLineVO;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeNodeVO;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeVO;
import cn.qijiv.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.qijiv.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 默认决策树执行引擎。 */
@Slf4j
public class DecisionTreeEngine implements IDecisionTreeEngine {

    /** 规则Key到业务节点实现的路由表。 */
    private final Map<String, ILogicTreeNode> logicTreeNodeGroup;
    /** 本次执行使用的树结构配置。 */
    private final RuleTreeVO ruleTreeVO;

    public DecisionTreeEngine(
            Map<String, ILogicTreeNode> logicTreeNodeGroup,
            RuleTreeVO ruleTreeVO) {
        if (logicTreeNodeGroup == null) {
            throw new IllegalArgumentException("规则树节点组不能为空");
        }
        if (ruleTreeVO == null
                || StringUtils.isBlank(ruleTreeVO.getTreeRootRuleNode())
                || ruleTreeVO.getTreeNodeMap() == null
                || ruleTreeVO.getTreeNodeMap().isEmpty()) {
            throw new IllegalArgumentException("规则树配置不完整");
        }
        this.logicTreeNodeGroup = logicTreeNodeGroup;
        this.ruleTreeVO = ruleTreeVO;
    }

    /** 执行规则树。 */
    @Override
    public DefaultTreeFactory.StrategyAwardVO process(
            String userId, Long strategyId, Integer awardId) {
        if (StringUtils.isBlank(userId) || strategyId == null || awardId == null) {
            throw new IllegalArgumentException("规则树执行参数不能为空");
        }

        String nextNode = ruleTreeVO.getTreeRootRuleNode();
        DefaultTreeFactory.StrategyAwardVO strategyAwardVO = null;
        // 记录本次执行经过的节点，避免错误配置导致无限循环。
        Set<String> visitedNodes = new HashSet<>();

        while (nextNode != null) {
            if (!visitedNodes.add(nextNode)) {
                throw new IllegalStateException("规则树存在环路，node: " + nextNode);
            }

            RuleTreeNodeVO ruleTreeNode = ruleTreeVO.getTreeNodeMap().get(nextNode);
            if (ruleTreeNode == null) {
                throw new IllegalStateException("规则树节点配置不存在，node: " + nextNode);
            }
            ILogicTreeNode logicTreeNode = logicTreeNodeGroup.get(ruleTreeNode.getRuleKey());
            if (logicTreeNode == null) {
                throw new IllegalStateException(
                        "规则树节点实现未注册，ruleKey: " + ruleTreeNode.getRuleKey());
            }

            // 节点只计算业务结果；如何跳转到下一节点由引擎统一处理。
            DefaultTreeFactory.TreeActionEntity action = logicTreeNode.logic(
                    userId, strategyId, awardId, ruleTreeNode.getRuleValue());
            if (action == null || action.getRuleLogicCheckType() == null) {
                throw new IllegalStateException(
                        "规则树节点未返回有效结果，ruleKey: " + ruleTreeNode.getRuleKey());
            }
            if (action.getStrategyAwardVO() != null) {
                // 后续节点可覆盖前面节点给出的接管数据，以最终叶子节点结果为准。
                strategyAwardVO = action.getStrategyAwardVO();
            }

            RuleLogicCheckTypeVO checkType = action.getRuleLogicCheckType();
            log.info("决策树引擎 treeId:{} node:{} ruleKey:{} result:{}",
                    ruleTreeVO.getTreeId(), nextNode, ruleTreeNode.getRuleKey(), checkType.getCode());
            String resolvedNextNode = nextNode(ruleTreeNode, checkType);
            if (RuleLogicCheckTypeVO.TAKE_OVER == checkType
                    && resolvedNextNode == null
                    && action.getStrategyAwardVO() == null) {
                throw new IllegalStateException(
                        "规则树接管结果缺少后续分支或奖品，ruleKey: " + ruleTreeNode.getRuleKey()
                                + ", result: " + checkType.getCode());
            }
            nextNode = resolvedNextNode;
        }

        return strategyAwardVO;
    }

    private String nextNode(RuleTreeNodeVO ruleTreeNode, RuleLogicCheckTypeVO checkType) {
        List<RuleTreeNodeLineVO> lines = ruleTreeNode.getTreeNodeLineVOList();
        if (lines == null || lines.isEmpty()) {
            // 没有出边的节点天然是叶子节点。
            return null;
        }
        for (RuleTreeNodeLineVO line : lines) {
            if (line == null || line.getRuleLimitType() == null || line.getRuleLimitValue() == null) {
                throw new IllegalStateException(
                        "规则树连线配置不完整，ruleKey: " + ruleTreeNode.getRuleKey());
            }
            if (line.getRuleLimitType().matches(
                    checkType.getCode(), line.getRuleLimitValue().getCode())) {
                // 目标节点为空也表示当前分支到此结束。
                return StringUtils.trimToNull(line.getRuleNodeTo());
            }
        }
        // 当前结果没有配置出边时，该节点就是此分支的叶子节点。
        return null;
    }
}
