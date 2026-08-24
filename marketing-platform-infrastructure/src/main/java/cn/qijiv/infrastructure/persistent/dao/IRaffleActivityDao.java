package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.RaffleActivityPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 抽奖活动 DAO
 *
 * @author jinlujia
 * @since 2026-07-28
 */
@Mapper
public interface IRaffleActivityDao {

    /**
     * 按活动ID查询抽奖活动。
     *
     * @param activityId 活动ID
     * @return 抽奖活动
     */
    RaffleActivityPO queryRaffleActivityByActivityId(@Param("activityId") Long activityId);

    /**
     * 按活动ID查询策略ID。
     *
     * @param activityId 活动ID
     * @return 策略ID
     */
    Long queryStrategyIdByActivityId(Long activityId);

    /**
     * 按策略ID查询活动ID。
     *
     * @param strategyId 策略ID
     * @return 活动ID
     */
    Long queryActivityIdByStrategyId(Long strategyId);
}
