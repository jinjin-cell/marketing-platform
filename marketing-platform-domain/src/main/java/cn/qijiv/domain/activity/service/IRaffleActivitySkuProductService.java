package cn.qijiv.domain.activity.service;

import cn.qijiv.domain.activity.model.entity.SkuProductEntity;

import java.util.List;

/**
 * @author qijiv
 * @since 2026/09/05
 */
public interface IRaffleActivitySkuProductService {

    /**
     * 查询当前活动ID下，创建的 sku 商品。「sku可以兑换活动抽奖次数」
     * @param activityId 活动ID
     * @return 返回sku商品集合
     */
    List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId);

}

