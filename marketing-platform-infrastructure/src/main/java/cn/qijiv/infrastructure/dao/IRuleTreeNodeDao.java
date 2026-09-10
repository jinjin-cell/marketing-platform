package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.dao.po.RuleTreeNodePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 规则树节点 DAO。 */
@Mapper
public interface IRuleTreeNodeDao {

    /**
     * 按规则树ID查询规则树节点列表。
     *
     * @param treeId 规则树ID
     * @return 规则树节点列表
     */
    List<RuleTreeNodePO> queryRuleTreeNodeListByTreeId(String treeId);

    /**
     * 批量查询规则树中的次数解锁节点（rule_lock）。
     *
     * @param treeIds 规则树ID列表
     * @return 次数解锁节点列表
     */
    List<RuleTreeNodePO> queryRuleLockNodeListByTreeIds(@Param("treeIds") List<String> treeIds);
}
