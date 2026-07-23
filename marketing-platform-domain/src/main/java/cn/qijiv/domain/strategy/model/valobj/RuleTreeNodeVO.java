package cn.qijiv.domain.strategy.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 规则树节点。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleTreeNodeVO {

    /** 规则树ID */
    private String treeId;
    /** 规则节点Key，对应 Spring 中的节点实现名称 */
    private String ruleKey;
    /** 规则节点描述 */
    private String ruleDesc;
    /** 规则节点配置值 */
    private String ruleValue;
    /** 从当前节点出发的分支 */
    private List<RuleTreeNodeLineVO> treeNodeLineVOList;
}
