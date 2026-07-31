package cn.qijiv.domain.activity.repository;

import cn.qijiv.domain.activity.model.entity.ActivitySkuEntity;
import cn.qijiv.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.qijiv.domain.activity.model.entity.ActivityEntity;
import cn.qijiv.domain.activity.model.aggregate.CreateOrderAggregate;
import cn.qijiv.domain.activity.model.entity.ActivityCountEntity;

import java.util.Date;

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

    String queryOrderIdByOutBusinessNo(String userId, String outBusinessNo);

    void doSaveOrder(CreateOrderAggregate createOrderAggregate);

    void cacheActivitySkuStockCount(String cacheKey, Integer stockCount);

    boolean subtractionActivitySkuStock(Long sku, String cacheKey, Date endDateTime);

    void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO activitySkuStockKeyVO);

    ActivitySkuStockKeyVO takeQueueValue();

    void clearQueueValue();

    void updateActivitySkuStock(Long sku);

    void clearActivitySkuStock(Long sku);



}

