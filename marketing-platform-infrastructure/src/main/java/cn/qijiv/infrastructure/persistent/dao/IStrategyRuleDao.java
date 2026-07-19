package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.StrategyRulePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 按策略ID和规则模型查询单条策略规则。
     *
     * @param strategyId 策略ID
     * @param ruleModel  规则模型，例如 rule_weight
     * @return 策略规则；不存在时返回null
     */
    StrategyRulePO queryStrategyRule(@Param("strategyId") Long strategyId,
                                     @Param("ruleModel") String ruleModel);

}
