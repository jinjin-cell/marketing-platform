package cn.qijiv.domain.strategy.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** 规则树值对象，描述一棵规则树的节点及其跳转关系。 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleTreeVO {

    /** 规则树ID */
    private String treeId;
    /** 规则树名称 */
    private String treeName;
    /** 规则树描述 */
    private String treeDesc;
    /** 根节点Key */
    private String treeRootRuleNode;
    /** 节点Key与节点配置的映射 */
    private Map<String, RuleTreeNodeVO> treeNodeMap;
}
