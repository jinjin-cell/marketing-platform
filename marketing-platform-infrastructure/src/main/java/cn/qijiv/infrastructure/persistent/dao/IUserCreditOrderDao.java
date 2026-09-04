package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.db.annotation.DBRouter;
import cn.qijiv.infrastructure.persistent.db.annotation.DBRouterStrategy;
import cn.qijiv.infrastructure.persistent.po.UserCreditOrderPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户积分订单数据访问层
 *
 * @author qijiv
 * @since 2026-09-03
 */
@Mapper
@DBRouterStrategy(splitTable = true)
public interface IUserCreditOrderDao {

    /**
     * 新增用户积分订单
     *
     * @param userCreditOrder 用户积分订单
     */
    void insert(UserCreditOrderPO userCreditOrder);

    /**
     * 按用户ID与外部业务号查询积分订单，用于防重校验
     *
     * @param userCreditOrderReq 查询条件（携带 userId 与 outBusinessNo）
     * @return 积分订单；不存在时返回 null
     */
    @DBRouter
    UserCreditOrderPO queryUserCreditOrderByOutBusinessNo(UserCreditOrderPO userCreditOrderReq);
}
