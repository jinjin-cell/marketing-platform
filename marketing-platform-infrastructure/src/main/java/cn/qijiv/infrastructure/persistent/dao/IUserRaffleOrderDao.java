package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.db.annotation.DBRouter;
import cn.qijiv.infrastructure.persistent.db.annotation.DBRouterStrategy;
import cn.qijiv.infrastructure.persistent.po.UserRaffleOrderPO;
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



}
