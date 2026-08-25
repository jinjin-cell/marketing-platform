package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.StrategyAwardPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 抽奖策略奖品 DAO
 *
 * @author jinlujia
 * @since 2026-07-18
 */
@Mapper
public interface IStrategyAwardDao {

    /**
     * 按策略ID查询策略奖品完整列表。
     *
     * @param strategyId 策略ID
     * @return 策略奖品列表
     */
    List<StrategyAwardPO> queryStrategyAwardListByStrategyId(Long strategyId);

    /** 按策略和奖品查询规则树模型字段。 */
    StrategyAwardPO queryStrategyAwardRuleModel(
            @org.apache.ibatis.annotations.Param("strategyId") Long strategyId,
            @org.apache.ibatis.annotations.Param("awardId") Integer awardId);

    /** 库存大于0时原子扣减1，返回受影响行数。 */
    int subtractionAwardStock(
            @org.apache.ibatis.annotations.Param("strategyId") Long strategyId,
            @org.apache.ibatis.annotations.Param("awardId") Integer awardId);

}
