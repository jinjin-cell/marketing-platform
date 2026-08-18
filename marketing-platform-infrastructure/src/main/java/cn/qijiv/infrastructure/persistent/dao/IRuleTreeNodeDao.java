package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.RuleTreeNodePO;
import org.apache.ibatis.annotations.Mapper;

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
}
