package cn.qijiv.infrastructure.persistent.po;

import lombok.Data;

import java.util.Date;

/** 规则树节点连线持久化对象，对应数据库表 rule_tree_node_line。 */
@Data
public class RuleTreeNodeLinePO {

    private Long id;
    private String treeId;
    private String ruleNodeFrom;
    private String ruleNodeTo;
    /** 分支比较方式，例如 EQUAL。 */
    private String ruleLimitType;
    /** 分支期望的节点结果，例如 ALLOW 或 TAKE_OVER。 */
    private String ruleLimitValue;
    private Date createTime;
    private Date updateTime;
}
