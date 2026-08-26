package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.db.annotation.DBRouterStrategy;
import cn.qijiv.infrastructure.persistent.po.UserBehaviorRebateOrderPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户行为返利订单数据访问层
 *
 * @author qijiv
 * @since 2026-08-26
 */
@Mapper
@DBRouterStrategy(splitTable = true)
public interface IUserBehaviorRebateOrderDao {

    /**
     * 新增用户行为返利订单
     *
     * @param userBehaviorRebateOrder 用户行为返利订单
     */
    void insert(UserBehaviorRebateOrderPO userBehaviorRebateOrder);

}
