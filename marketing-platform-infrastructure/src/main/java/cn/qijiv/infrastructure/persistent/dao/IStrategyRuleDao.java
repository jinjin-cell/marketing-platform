package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.StrategyRulePO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 抽奖策略规则 DAO
 *
 * @author jinlujia
 * @date 2026/07/18
 */
@Mapper
public interface IStrategyRuleDao {

    /**
     * 查询策略规则列表
     *
     * @param strategyId 策略ID
     * @return 策略规则列表
     */
    List<StrategyRulePO> queryStrategyRuleList(Long strategyId);

}
