package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.dao.po.RuleTreePO;
import org.apache.ibatis.annotations.Mapper;

/** 规则树根配置 DAO。DAO 负责把数据库记录读取为 PO。 */
@Mapper
public interface IRuleTreeDao {

    /**
     * 按规则树ID查询规则树根配置。
     *
     * @param treeId 规则树ID
     * @return 规则树根配置；不存在时返回null
     */
    RuleTreePO queryRuleTreeByTreeId(String treeId);
}
