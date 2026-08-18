package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.RaffleActivityCountPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 抽奖活动次数配置 DAO
 *
 * @author jinlujia
 * @since 2026-07-28
 */
@Mapper
public interface IRaffleActivityCountDao {

    /**
     * 按活动次数配置ID查询活动次数配置。
     *
     * @param activityCountId 活动次数配置ID
     * @return 活动次数配置
     */
    RaffleActivityCountPO queryRaffleActivityCountByActivityCountId(
            @Param("activityCountId") Long activityCountId);

}
