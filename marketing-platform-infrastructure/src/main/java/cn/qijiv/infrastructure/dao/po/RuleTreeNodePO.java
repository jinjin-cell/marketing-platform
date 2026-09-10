package cn.qijiv.infrastructure.dao.po;

import lombok.Data;

import java.util.Date;

/** 规则树节点持久化对象，对应数据库表 rule_tree_node。 */
@Data
public class RuleTreeNodePO {

    /** 自增ID */
    private Long id;
    /** 规则树ID，关联 rule_tree 表 */
    private String treeId;
    /** 节点业务Key，同时对应 Spring 节点 Bean 名称。 */
    private String ruleKey;
    /** 节点描述 */
    private String ruleDesc;
    /** 节点执行所需的配置值，例如解锁次数或兜底积分范围。 */
    private String ruleValue;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
