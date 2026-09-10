package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.db.annotation.DBRouter;
import cn.qijiv.infrastructure.dao.po.RaffleActivityAccountMonthPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 抽奖活动用户月次数数据访问层
 * @author jinlujia
 * @since 2026-07-27
 */
@Mapper
public interface IRaffleActivityAccountMonthDao {

    @DBRouter
    RaffleActivityAccountMonthPO queryActivityAccountMonthByUserId(RaffleActivityAccountMonthPO raffleActivityAccountMonthReq);

    int updateActivityAccountMonthSubtractionQuota(RaffleActivityAccountMonthPO raffleActivityAccountMonth);

    void insertActivityAccountMonth(RaffleActivityAccountMonthPO raffleActivityAccountMonth);

    /** 更新当前月份账户的总额度和剩余额度；不存在时不更新。 */
    int addAccountQuota(RaffleActivityAccountMonthPO raffleActivityAccountMonth);


}
