package cn.qijiv.infrastructure.persistent.po;

import lombok.Data;

import java.util.Date;

/** 规则树根配置持久化对象，对应数据库表 rule_tree。 */
@Data
public class RuleTreePO {

    private Long id;
    /** 规则树业务ID，例如 tree_lock。 */
    private String treeId;
    private String treeName;
    private String treeDesc;
    /** 规则树开始执行的第一个节点Key。 */
    private String treeNodeRuleKey;
    private Date createTime;
    private Date updateTime;
}
