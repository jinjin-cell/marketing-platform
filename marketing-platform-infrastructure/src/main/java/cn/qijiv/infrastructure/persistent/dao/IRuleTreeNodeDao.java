package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.RuleTreeNodePO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 规则树节点 DAO。 */
@Mapper
public interface IRuleTreeNodeDao {

    List<RuleTreeNodePO> queryRuleTreeNodeListByTreeId(String treeId);
}
