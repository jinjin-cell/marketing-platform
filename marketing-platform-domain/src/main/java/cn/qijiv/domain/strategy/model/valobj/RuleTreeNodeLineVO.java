package cn.qijiv.domain.strategy.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 规则树节点连线。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleTreeNodeLineVO {

    /** 规则树ID */
    private String treeId;
    /** 连线起始节点 */
    private String ruleNodeFrom;
    /** 连线目标节点；为空表示执行结束 */
    private String ruleNodeTo;
    /** 连线匹配关系 */
    private RuleLimitTypeVO ruleLimitType;
    /** 连线期望的规则执行结果 */
    private RuleLogicCheckTypeVO ruleLimitValue;
}
