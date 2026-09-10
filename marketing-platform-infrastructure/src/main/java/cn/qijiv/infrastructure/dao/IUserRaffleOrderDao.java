package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.db.annotation.DBRouter;
import cn.qijiv.infrastructure.db.annotation.DBRouterStrategy;
import cn.qijiv.infrastructure.dao.po.UserRaffleOrderPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户抽奖订单数据访问层
 * @author jinlujia
 * @since 2026-07-27
 */
@Mapper
@DBRouterStrategy(splitTable = true)
public interface IUserRaffleOrderDao {

    void insert(UserRaffleOrderPO userRaffleOrder);

    @DBRouter
    UserRaffleOrderPO queryNoUsedRaffleOrder(UserRaffleOrderPO userRaffleOrderReq);


    /**
     * 更新用户抽奖订单状态为已使用
     *
     * @param userRaffleOrderReq 用户抽奖订单请求参数
     * @return 更新影响的行数
     */
    int updateUserRaffleOrderStateUsed(UserRaffleOrderPO userRaffleOrderReq);
}
