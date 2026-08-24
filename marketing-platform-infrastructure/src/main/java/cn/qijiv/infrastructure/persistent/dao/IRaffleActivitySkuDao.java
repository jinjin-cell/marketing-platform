package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.RaffleActivitySkuPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 抽奖活动SKU DAO
 *
 * @author jinlujia
 * @since 2026-07-28
 */
@Mapper
public interface IRaffleActivitySkuDao {

    /**
     * 按SKU查询活动SKU信息。
     *
     * @param sku 商品SKU
     * @return 活动SKU信息
     */
    RaffleActivitySkuPO queryRaffleActivitySkuBySku(@Param("sku") Long sku);

    /**
     * 剩余库存大于0时原子扣减SKU库存1，返回受影响的行数。
     *
     * @param sku 商品SKU
     * @return 受影响的行数
     */
    int updateActivitySkuStock(@Param("sku") Long sku);

    /**
     * 将SKU剩余库存清零。
     *
     * @param sku 商品SKU
     * @return 受影响的行数
     */
    int clearActivitySkuStock(@Param("sku") Long sku);

    List<RaffleActivitySkuPO> queryActivitySkuListByActivityId(Long activityId);
}
