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
     * 查询策略奖品列表
     *
     * @param strategyId 策略ID
     * @return 策略奖品列表
     */
    List<StrategyAwardPO> queryStrategyAwardList(Long strategyId);

    List<StrategyAwardPO> queryStrategyAwardListByStrategyId(Long strategyId);

}
