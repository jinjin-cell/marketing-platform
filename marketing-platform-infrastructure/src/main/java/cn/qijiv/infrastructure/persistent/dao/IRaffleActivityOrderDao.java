package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.RaffleActivityOrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
    void insert(RaffleActivityOrderPO raffleActivityOrder);

    /**
     * 按用户ID查询活动订单列表。
     *
     * @param userId 用户ID
     * @return 活动订单列表
     */
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

}
