package cn.qijiv.domain.activity.repository;

import cn.qijiv.domain.activity.model.entity.ActivitySkuEntity;
import cn.qijiv.domain.activity.model.entity.ActivityEntity;
import cn.qijiv.domain.activity.model.aggregate.CreateOrderAggregate;
import cn.qijiv.domain.activity.model.entity.ActivityCountEntity;

/**
 * 活动仓库接口
 * 
 * @author qijiv
 * @since 2026/7/18
 */
public interface IActivityRepository {

    ActivitySkuEntity queryActivitySku(Long sku);

    ActivityEntity queryRaffleActivityByActivityId(Long activityId);

    ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId);

    void doSaveOrder(CreateOrderAggregate createOrderAggregate);


}

