package cn.qijiv.domain.award.respository;

import cn.qijiv.domain.award.model.aggregate.GiveOutPrizesAggregate;
import cn.qijiv.domain.award.model.aggregate.UserAwardRecordAggregate;
import cn.qijiv.domain.award.model.entity.UserAwardRecordEntity;

import java.util.List;

/**
 * 奖品仓储服务
 *
 * @author qijiv
 * @since 2026-08-19
 */
public interface IAwardRepository {

    /**
     * 保存用户中奖记录
     *
     * @param userAwardRecordAggregate 用户中奖记录聚合对象
     */
    void saveUserAwardRecord(UserAwardRecordAggregate userAwardRecordAggregate);

    /**
     * 保存抽奖结果
     *
     * @param giveOutPrizesAggregate 抽奖结果聚合对象
     */
    void saveGiveOutPrizesAggregate(GiveOutPrizesAggregate giveOutPrizesAggregate);

    /**
     * 查询奖品配置
     *
     * @param awardId 奖品ID
     * @return 奖品配置
     */
    String queryAwardConfig(Integer awardId);


    /**
     * 查询奖品key
     *
     * @param awardId 奖品ID
     * @return 奖品key
     */
    String queryAwardKey(Integer awardId);

    /**
     * 查询用户中奖记录列表
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @return 用户中奖记录列表
     */
    List<UserAwardRecordEntity> queryUserAwardRecordList(String userId, Long activityId);


}
