package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.db.annotation.DBRouter;
import cn.qijiv.infrastructure.persistent.po.RaffleActivityAccountDayPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 抽奖活动用户日次数数据访问层
 * @author jinlujia
 * @since 2026-07-27
 */
@Mapper
public interface IRaffleActivityAccountDayDao {

    @DBRouter
    RaffleActivityAccountDayPO queryActivityAccountDayByUserId(RaffleActivityAccountDayPO raffleActivityAccountDayReq);

    int updateActivityAccountDaySubtractionQuota(RaffleActivityAccountDayPO raffleActivityAccountDay);

    void insertActivityAccountDay(RaffleActivityAccountDayPO raffleActivityAccountDay);

}
