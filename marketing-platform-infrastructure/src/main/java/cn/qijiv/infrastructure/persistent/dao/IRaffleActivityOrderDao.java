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

    int insert(RaffleActivityOrderPO raffleActivityOrder);

    List<RaffleActivityOrderPO> queryRaffleActivityOrderByUserId(@Param("userId") String userId);

}
