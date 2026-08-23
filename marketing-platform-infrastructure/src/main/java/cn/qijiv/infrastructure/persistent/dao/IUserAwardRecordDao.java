package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.db.annotation.DBRouterStrategy;
import cn.qijiv.infrastructure.persistent.po.UserAwardRecordPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户中奖记录数据访问层
 * @author jinlujia
 * @since 2026-07-27
 */
@Mapper
@DBRouterStrategy(splitTable = true)
public interface IUserAwardRecordDao {

    void insert(UserAwardRecordPO userAwardRecord);
}
