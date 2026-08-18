package cn.qijiv.infrastructure.persistent.po;

import lombok.Data;

import java.util.Date;

/** 规则树节点连线持久化对象，对应数据库表 rule_tree_node_line。 */
@Data
public class RuleTreeNodeLinePO {

    /** 自增ID */
    private Long id;
    /** 规则树ID，关联 rule_tree 表 */
    private String treeId;
    /** 起始节点Key，关联规则树节点 */
    private String ruleNodeFrom;
    /** 目标节点Key，关联规则树节点 */
    private String ruleNodeTo;
    /** 分支比较方式，例如 EQUAL。 */
    private String ruleLimitType;
    /** 分支期望的节点结果，例如 ALLOW 或 TAKE_OVER。 */
    private String ruleLimitValue;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
