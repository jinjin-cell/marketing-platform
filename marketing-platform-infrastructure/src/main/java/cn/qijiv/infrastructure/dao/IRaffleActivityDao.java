package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.dao.po.RaffleActivityPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 查询全部活动配置（活动ID升序），用于前端活动列表与多活动切换。
     *
     * @return 活动列表
     */
    List<RaffleActivityPO> queryRaffleActivityList();
}
