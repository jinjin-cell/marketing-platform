package cn.qijiv.domain.activity.service;

import cn.qijiv.domain.activity.model.entity.ActivityShopCartEntity;
import cn.qijiv.domain.activity.model.entity.ActivityOrderEntity;

public interface IRaffleOrder {

    /**
     * 以sku创建抽奖活动订单，获得参与抽奖资格（可消耗的次数）
     *
     * @param activityShopCartEntity 活动sku实体，通过sku领取活动。
     * @return 活动参与记录实体
     */
    ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity);

}

