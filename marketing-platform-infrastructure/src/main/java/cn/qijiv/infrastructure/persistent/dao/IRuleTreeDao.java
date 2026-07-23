package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.RuleTreePO;
import org.apache.ibatis.annotations.Mapper;

/** 规则树根配置 DAO。DAO 负责把数据库记录读取为 PO。 */
@Mapper
public interface IRuleTreeDao {

    RuleTreePO queryRuleTreeByTreeId(String treeId);
}
