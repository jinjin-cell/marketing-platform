package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.db.annotation.DBRouterStrategy;
import cn.qijiv.infrastructure.dao.po.UserAwardRecordPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户中奖记录数据访问层
 * @author jinlujia
 * @since 2026-07-27
 */
@Mapper
@DBRouterStrategy(splitTable = true)
public interface IUserAwardRecordDao {

    /**
     * 插入用户中奖记录
     *
     * @param userAwardRecord 用户中奖记录
     */
    void insert(UserAwardRecordPO userAwardRecord);

    /**
     * 查询用户中奖记录列表（按活动过滤，倒序）
     *
     * @param userAwardRecordReq 查询条件「用户ID、活动ID」
     * @return 用户中奖记录列表
     */
    List<UserAwardRecordPO> queryUserAwardRecordList(UserAwardRecordPO userAwardRecordReq);

    /**
     * 更新用户中奖记录完成状态
     *
     * @param userAwardRecordReq 用户中奖记录更新条件
     * @return 影响行数
     */
    int updateAwardRecordCompletedState(UserAwardRecordPO userAwardRecordReq);

}
