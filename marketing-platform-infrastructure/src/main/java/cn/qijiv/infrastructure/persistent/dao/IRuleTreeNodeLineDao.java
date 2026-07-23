package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.RuleTreeNodeLinePO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 规则树节点连线 DAO。 */
@Mapper
public interface IRuleTreeNodeLineDao {

    List<RuleTreeNodeLinePO> queryRuleTreeNodeLineListByTreeId(String treeId);
}
