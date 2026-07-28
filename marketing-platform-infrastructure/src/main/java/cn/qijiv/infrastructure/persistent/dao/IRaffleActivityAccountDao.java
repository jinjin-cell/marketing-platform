package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.RaffleActivityAccountPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 抽奖活动账户 DAO
 *
 * @author jinlujia
 * @since 2026-07-28
 */
@Mapper
public interface IRaffleActivityAccountDao {

    RaffleActivityAccountPO queryRaffleActivityAccount(
            @Param("userId") String userId,
            @Param("activityId") Long activityId);

}
