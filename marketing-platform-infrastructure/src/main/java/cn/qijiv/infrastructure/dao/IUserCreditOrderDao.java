package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.db.annotation.DBRouter;
import cn.qijiv.infrastructure.db.annotation.DBRouterStrategy;
import cn.qijiv.infrastructure.dao.po.UserCreditOrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 按用户ID查询积分流水（交易时间倒序）
     *
     * @param userId 用户ID
     * @param limit  最大返回条数
     * @return 积分订单列表
     */
    @DBRouter
    List<UserCreditOrderPO> queryUserCreditOrderList(@Param("userId") String userId,
                                                     @Param("limit") Integer limit);
}
