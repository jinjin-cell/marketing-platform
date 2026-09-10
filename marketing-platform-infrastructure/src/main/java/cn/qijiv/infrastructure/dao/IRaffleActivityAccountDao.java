package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.db.annotation.DBRouter;
import cn.qijiv.infrastructure.dao.po.RaffleActivityAccountPO;
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

    /**
     * 更新活动账户的抽奖次数（总/日/月）及其剩余次数。
     *
     * @param raffleActivityAccount 活动账户信息
     * @return 受影响的行数
     */
    int updateAccountQuota(RaffleActivityAccountPO raffleActivityAccount);

    /**
     * 新增一条活动账户记录。
     *
     * @param raffleActivityAccount 活动账户信息
     */
    void insert(RaffleActivityAccountPO raffleActivityAccount);

    @DBRouter
    RaffleActivityAccountPO queryActivityAccountByUserId(RaffleActivityAccountPO raffleActivityAccountReq);

    /**
     * 减少活动账户的抽奖次数（总/日/月）及其剩余次数。
     *
     * @param raffleActivityAccount 活动账户信息
     * @return 受影响的行数
     */
    int updateActivityAccountSubtractionQuota(RaffleActivityAccountPO raffleActivityAccount);

    /**
     * 更新活动账户的月剩余图片抽奖次数。
     *
     * @param raffleActivityAccount 活动账户信息
     */
    void updateActivityAccountMonthSurplusImageQuota(RaffleActivityAccountPO raffleActivityAccount);

    /**
     * 更新活动账户的日剩余图片抽奖次数。
     *
     * @param raffleActivityAccount 活动账户信息
     */
    void updateActivityAccountDaySurplusImageQuota(RaffleActivityAccountPO raffleActivityAccount);

    /**
     * 根据用户ID和活动ID查询活动账户信息。
     *
     * @param userId    用户ID
     * @param activityId 活动ID
     * @return 活动账户信息
     */
    RaffleActivityAccountPO queryAccountByUserId(@Param("userId") String userId, @Param("activityId") Long activityId);
}
