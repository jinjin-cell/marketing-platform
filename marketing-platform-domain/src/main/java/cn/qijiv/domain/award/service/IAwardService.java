package cn.qijiv.domain.award.service;

import cn.qijiv.domain.award.model.entity.DistributeAwardEntity;
import cn.qijiv.domain.award.model.entity.UserAwardRecordEntity;

import java.util.List;

/**
 * 奖品服务接口
 *
 * @author qijiv
 * @since 2026-08-19
 */
public interface IAwardService {


    /**
     * 保存用户中奖记录
     *
     * @param userAwardRecordEntity 用户中奖记录实体
     */
    void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity);

    /**
     * 配送发货奖品
     */
    void distributeAward(DistributeAwardEntity distributeAwardEntity);

    /**
     * 查询用户中奖记录列表
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @return 用户中奖记录列表
     */
    List<UserAwardRecordEntity> queryUserAwardRecordList(String userId, Long activityId);


}

