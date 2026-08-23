package cn.qijiv.domain.award.respository;

import cn.qijiv.domain.award.model.aggregate.UserAwardRecordAggregate;

/**
 * 奖品仓储服务
 *
 * @author qijiv
 * @since 2026-08-19
 */
public interface IAwardRepository {

    void saveUserAwardRecord(UserAwardRecordAggregate userAwardRecordAggregate);

}
