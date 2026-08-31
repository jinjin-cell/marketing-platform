package cn.qijiv.domain.activity.repository;

import cn.qijiv.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import cn.qijiv.domain.activity.model.entity.*;
import cn.qijiv.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.qijiv.domain.activity.model.aggregate.CreateQuotaOrderAggregate;

import java.util.Date;
import java.util.List;

/**
 * 活动仓库接口
 * 
 * @author qijiv
 * @since 2026/7/18
 */
public interface IActivityRepository {




    /**
     * 查询活动SKU信息
     *
     * @param sku 活动商品SKU
     * @return 活动SKU实体
     */
    ActivitySkuEntity queryActivitySku(Long sku);

    /**
     * 查询抽奖活动信息
     *
     * @param activityId 活动ID
     * @return 活动实体
     */
    ActivityEntity queryRaffleActivityByActivityId(Long activityId);

    /**
     * 查询抽奖活动次数配置信息
     *
     * @param activityCountId 活动次数配置ID
     * @return 活动次数配置实体
     */
    ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId);

    /**
     * 根据外部业务单号查询订单ID
     *
     * @param userId 用户ID
     * @param outBusinessNo 外部业务单号
     * @return 订单ID
     */
    String queryOrderIdByOutBusinessNo(String userId, String outBusinessNo);

    /**
     * 保存活动充值订单
     *
     * @param createQuotaOrderAggregate 创建充值订单聚合对象
     */
    void doSaveOrder(CreateQuotaOrderAggregate createQuotaOrderAggregate);

    /**
     * 缓存活动SKU库存数量
     *
     * @param cacheKey 缓存Key
     * @param stockCount 库存数量
     */
    void cacheActivitySkuStockCount(String cacheKey, Integer stockCount);

    /**
     * 扣减活动SKU库存
     *
     * @param sku 活动商品SKU
     * @param cacheKey 缓存Key
     * @param endDateTime 活动结束时间
     * @return 扣减结果
     */
    boolean subtractionActivitySkuStock(Long sku, String cacheKey, Date endDateTime);

    /**
     * 活动SKU库存消耗后发送到队列
     *
     * @param activitySkuStockKeyVO 活动SKU库存Key信息
     */
    void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO activitySkuStockKeyVO);

    /**
     * 获取活动SKU库存消耗队列值
     *
     * @return 活动SKU库存Key信息
     */
    ActivitySkuStockKeyVO takeQueueValue();

    /**
     * 清空活动SKU库存消耗队列
     */
    void clearQueueValue();

    /**
     * 更新活动SKU库存
     *
     * @param sku 活动商品SKU
     */
    void updateActivitySkuStock(Long sku);

    /**
     * 清空活动SKU库存
     *
     * @param sku 活动商品SKU
     */
    void clearActivitySkuStock(Long sku);


    /**
     * 保存参与抽奖订单聚合对象
     *
     * @param createPartakeOrderAggregate 创建参与抽奖订单聚合对象
     */
    void saveCreatePartakeOrderAggregate(CreatePartakeOrderAggregate createPartakeOrderAggregate);

    /**
     * 根据用户ID、活动ID和天查询活动账户日实体
     *
     * @param userId 用户ID
     * @param activityId 活动ID
     * @param day 天
     * @return 活动账户日实体
     */
    ActivityAccountDayEntity queryActivityAccountDayByUserId(String userId, Long activityId, String day);

    /**
     * 根据用户ID和活动ID查询活动账户实体
     *
     * @param userId 用户ID
     * @param activityId 活动ID
     * @return 活动账户实体
     */
    ActivityAccountEntity queryActivityAccountByUserId(String userId, Long activityId);

    /**
     * 根据用户ID、活动ID和月查询活动账户月实体
     *
     * @param userId 用户ID
     * @param activityId 活动ID
     * @param month 月
     * @return 活动账户月实体
     */
    ActivityAccountMonthEntity queryActivityAccountMonthByUserId(String userId, Long activityId, String month);

    /**
     * 根据参与抽奖活动实体查询未使用的抽奖订单实体
     *
     * @param partakeRaffleActivityEntity 参与抽奖活动实体
     * @return 抽奖订单实体
     */
    UserRaffleOrderEntity queryNoUsedRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity);

    /**
     * 根据活动ID查询活动SKU列表
     *
     * @param activityId 活动ID
     * @return 活动SKU实体列表
     */
    List<ActivitySkuEntity> queryActivitySkuListByActivityId(Long activityId);

    /**
     * 根据活动ID和用户ID查询抽奖活动账户日实体的参与次数
     *
     * @param activityId 活动ID
     * @param userId 用户ID
     * @return 参与次数
     */
    Integer queryRaffleActivityAccountDayPartakeCount(Long activityId, String userId);

    /** 查询用户在活动下的总额度账户；不存在时返回 null。 */
    ActivityAccountEntity queryActivityAccountEntity(Long activityId, String userId);

    /** 查询用户在活动下已使用的总抽奖次数。 */
    Integer queryRaffleActivityAccountPartakeCount(Long activityId, String userId);
}

