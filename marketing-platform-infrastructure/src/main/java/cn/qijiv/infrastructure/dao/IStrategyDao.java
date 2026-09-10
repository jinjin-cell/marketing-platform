package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.dao.po.StrategyPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 抽奖策略 DAO
 *
 * @author jinlujia
 * @since 2026-07-18
 */
@Mapper
public interface IStrategyDao {

    /**
     * 查询抽奖策略
     *
     * @param strategyId 策略ID
     * @return 抽奖策略
     */
    StrategyPO queryStrategyByStrategyId(Long strategyId);

}
