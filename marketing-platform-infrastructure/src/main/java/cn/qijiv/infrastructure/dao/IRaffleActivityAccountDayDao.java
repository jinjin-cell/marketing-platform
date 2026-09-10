package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.db.annotation.DBRouter;
import cn.qijiv.infrastructure.dao.po.RaffleActivityAccountDayPO;
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

    /** 更新当前日期账户的总额度和剩余额度；不存在时不更新。 */
    int addAccountQuota(RaffleActivityAccountDayPO raffleActivityAccountDay);

}
