package cn.qijiv.domain.strategy.service.rule.tree.factory;

import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeVO;
import cn.qijiv.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.qijiv.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import cn.qijiv.domain.strategy.service.rule.tree.factory.engine.impl.DecisionTreeEngine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 规则树工厂，管理节点注册并为单棵规则树创建执行引擎。 */
@Service
public class DefaultTreeFactory {

    /**
     * Spring 按 Bean 名称注入全部规则树节点，例如 rule_lock、rule_stock。
     * Map 在工厂创建时固化，防止运行过程中节点路由被意外修改。
     */
    private final Map<String, ILogicTreeNode> logicTreeNodeGroup;

    public DefaultTreeFactory(Map<String, ILogicTreeNode> logicTreeNodeGroup) {
        this.logicTreeNodeGroup = Collections.unmodifiableMap(
                new LinkedHashMap<>(logicTreeNodeGroup));
    }

    /**
     * 为一份规则树配置创建独立执行引擎。
     *
     * @param ruleTreeVO 本次需要执行的规则树
     * @return 决策树执行引擎
     */
    public IDecisionTreeEngine openLogicTree(RuleTreeVO ruleTreeVO) {
        return new DecisionTreeEngine(logicTreeNodeGroup, ruleTreeVO);
    }

    /** 单个节点的执行结果。 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TreeActionEntity {

        /** 节点计算结果，用于选择下一条连线。 */
        private RuleLogicCheckTypeVO ruleLogicCheckType;
        /** 节点接管抽奖时返回的数据；普通放行节点可以不返回。 */
        private StrategyAwardVO strategyAwardVO;
    }

    /** 规则树接管抽奖后返回的奖品数据。 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategyAwardVO {

        /** 规则树最终指定的奖品ID。 */
        private Integer awardId;
        /** 奖品规则配置，例如随机积分范围。 */
        private String awardRuleValue;
    }
}
