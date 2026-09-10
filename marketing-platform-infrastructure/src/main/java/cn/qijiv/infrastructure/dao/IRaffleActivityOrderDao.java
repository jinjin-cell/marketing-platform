package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.db.annotation.DBRouter;
import cn.qijiv.infrastructure.dao.po.RaffleActivityOrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 抽奖活动订单 DAO
 *
 * @author jinlujia
 * @since 2026-07-28
 */
@Mapper
public interface IRaffleActivityOrderDao {

    /**
     * 新增一条活动订单记录。
     *
     * @param raffleActivityOrder 活动订单信息
     */
    @DBRouter
    void insert(RaffleActivityOrderPO raffleActivityOrder);

    /**
     * 按用户ID查询活动订单列表。
     *
     * @param userId 用户ID
     * @return 活动订单列表
     */
    @DBRouter
    List<RaffleActivityOrderPO> queryRaffleActivityOrderByUserId(@Param("userId") String userId);

    /**
     * 按用户ID和外部业务号查询单条活动订单。
     *
     * @param userId       用户ID
     * @param outBusinessNo 外部业务号
     * @return 活动订单；不存在时返回null
     */
    RaffleActivityOrderPO queryByOutBusinessNo(@Param("userId") String userId,
                                                @Param("outBusinessNo") String outBusinessNo);

    /**
     * 查询全部分表下的业务键（用户ID + 外部业务号），用于构建布隆过滤器超集。
     *
     * @return 业务键列表
     */
    List<RaffleActivityOrderPO> queryAllBusinessKeys();

    /**
     * 根据活动订单查询活动订单。
     *
     * @param raffleActivityOrderReq 活动订单信息
     * @return 活动订单；不存在时返回null
     */
    @DBRouter
    RaffleActivityOrderPO queryRaffleActivityOrder(RaffleActivityOrderPO raffleActivityOrderReq);

    /**
     * 更新订单状态为完成。
     *
     * @param raffleActivityOrderReq 活动订单信息
     * @return 影响行数
     */
    int updateOrderCompleted(RaffleActivityOrderPO raffleActivityOrderReq);

    /**
     * 查询未支付的活动订单。
     *
     * @param raffleActivityOrderReq 活动订单信息
     * @return 活动订单；不存在时返回null
     */
    @DBRouter
    RaffleActivityOrderPO queryUnpaidActivityOrder(RaffleActivityOrderPO raffleActivityOrderReq);

    /**
     * 将创建时间早于指定时间的「待支付」订单批量置为过期。
     * <p>
     * 该操作不带用户ID分片键，由 ShardingSphere 广播到全部分表执行。
     *
     * @param beforeTime 过期临界时间
     * @return 影响行数
     */
    int updateOrderExpired(@Param("beforeTime") Date beforeTime);
}
