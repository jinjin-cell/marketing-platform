package cn.qijiv.domain.award.service;

import cn.qijiv.domain.award.model.entity.UserAwardRecordEntity;

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

}

