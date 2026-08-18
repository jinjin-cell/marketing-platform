package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.RuleTreeNodeLinePO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 规则树节点连线 DAO。 */
@Mapper
public interface IRuleTreeNodeLineDao {

    /**
     * 按规则树ID查询规则树节点连线列表。
     *
     * @param treeId 规则树ID
     * @return 规则树节点连线列表
     */
    List<RuleTreeNodeLinePO> queryRuleTreeNodeLineListByTreeId(String treeId);
}
