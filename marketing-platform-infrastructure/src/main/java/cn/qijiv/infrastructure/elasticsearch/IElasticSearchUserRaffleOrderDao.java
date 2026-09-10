package cn.qijiv.infrastructure.elasticsearch;

import cn.qijiv.infrastructure.elasticsearch.po.UserRaffleOrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IElasticSearchUserRaffleOrderDao {

    /**
     * 查询 ES 中最近同步的用户抽奖订单，最多返回 1000 条。
     */
    List<UserRaffleOrderPO> queryUserRaffleOrderList();

    /**
     * 按用户查询 ES 中最近同步的抽奖订单，最多返回 1000 条。
     */
    List<UserRaffleOrderPO> queryUserRaffleOrderListByUserId(@Param("userId") String userId);

}
