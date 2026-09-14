package cn.qijiv.domain.activity.service.quota;

import cn.qijiv.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.qijiv.domain.activity.model.entity.*;
import cn.qijiv.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.qijiv.domain.activity.repository.IActivityRepository;
import cn.qijiv.domain.activity.service.IRaffleActivitySkuStockService;
import cn.qijiv.domain.activity.service.quota.policy.ITradePolicy;
import cn.qijiv.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.time.LocalDate;
import java.util.Map;

/**
 * 抽奖活动服务
 * @author qijiv
 * @since  2026/7/18
 */
@Service
public class RaffleActivityAccountQuotaService extends AbstractRaffleActivityAccountQuota implements IRaffleActivitySkuStockService {


    /**
     * 构造方法注入活动仓库与责任链工厂
     *
     * @param activityRepository          活动仓库
     * @param defaultActivityChainFactory 默认活动责任链工厂
     * @param tradePolicyGroup
     */
    public RaffleActivityAccountQuotaService(IActivityRepository activityRepository, DefaultActivityChainFactory defaultActivityChainFactory, Map<String, ITradePolicy> tradePolicyGroup) {
        super(activityRepository, defaultActivityChainFactory, tradePolicyGroup);
    }

    /**
     * 构建活动充值订单聚合对象
     *
     * @param skuRechargeEntity 活动商品充值实体对象
     * @param activitySkuEntity 活动SKU实体
     * @param activityEntity 活动实体
     * @param activityCountEntity 活动次数配置实体
     * @return 创建充值订单聚合对象
     */
    @Override
    protected CreateQuotaOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity) {
        // 订单实体对象
        ActivityOrderEntity activityOrderEntity = new ActivityOrderEntity();
        activityOrderEntity.setUserId(skuRechargeEntity.getUserId());
        activityOrderEntity.setSku(skuRechargeEntity.getSku());
        activityOrderEntity.setActivityId(activityEntity.getActivityId());
        activityOrderEntity.setActivityName(activityEntity.getActivityName());
        activityOrderEntity.setStrategyId(activityEntity.getStrategyId());
        // 公司里一般会有专门的雪花算法UUID服务，我们这里直接生成个12位就可以了。
        activityOrderEntity.setOrderId(RandomStringUtils.randomNumeric(12));
        activityOrderEntity.setOrderTime(new Date());
        activityOrderEntity.setTotalCount(activityCountEntity.getTotalCount());
        activityOrderEntity.setDayCount(activityCountEntity.getDayCount());
        activityOrderEntity.setMonthCount(activityCountEntity.getMonthCount());
        activityOrderEntity.setPayAmount(activitySkuEntity.getProductAmount());
        activityOrderEntity.setOutBusinessNo(skuRechargeEntity.getOutBusinessNo());

        // 构建聚合对象
        return CreateQuotaOrderAggregate.builder()
                .userId(skuRechargeEntity.getUserId())
                .activityId(activitySkuEntity.getActivityId())
                .totalCount(activityCountEntity.getTotalCount())
                .dayCount(activityCountEntity.getDayCount())
                .monthCount(activityCountEntity.getMonthCount())
                .activityOrderEntity(activityOrderEntity)
                .build();
    }


    /**
     * 获取活动SKU库存消耗队列值
     *
     * @return 活动SKU库存Key信息
     * @throws InterruptedException 中断异常
     */
    @Override
    public ActivitySkuStockKeyVO takeQueueValue() throws InterruptedException {
        return activityRepository.takeQueueValue();
    }

    /**
     * 清空活动SKU库存消耗队列
     */
    @Override
    public void clearQueueValue() {
        activityRepository.clearQueueValue();
    }

    /**
     * 更新活动SKU库存
     *
     * @param sku 活动商品SKU
     */
    @Override
    public void updateActivitySkuStock(Long sku) {
        activityRepository.updateActivitySkuStock(sku);
    }

    /**
     * 清空活动SKU库存
     *
     * @param sku 活动商品SKU
     */
    @Override
    public void clearActivitySkuStock(Long sku) {
        activityRepository.clearActivitySkuStock(sku);
    }


    /**
     * 更新订单
     *
     * @param deliveryOrderEntity 订单实体对象
     */
    @Override
    public void updateOrder(DeliveryOrderEntity deliveryOrderEntity) {
        activityRepository.updateOrder(deliveryOrderEntity);
    }

    /**
     * 查询用户在某活动今日已参与的抽奖次数
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 今日已参与次数
     */
    @Override
    public Integer queryRaffleActivityAccountDayPartakeCount(Long activityId, String userId) {
        return activityRepository.queryRaffleActivityAccountDayPartakeCount(activityId, userId);
    }

    @Override
    public ActivityAccountEntity queryActivityAccountEntity(Long activityId, String userId) {
        ActivityAccountEntity account = activityRepository.queryActivityAccountEntity(activityId, userId);
        if (account == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        ActivityAccountMonthEntity monthAccount = activityRepository.queryActivityAccountMonthByUserId(
                userId, activityId, today.toString().substring(0, 7));
        ActivityAccountDayEntity dayAccount = activityRepository.queryActivityAccountDayByUserId(
                userId, activityId, today.toString());

        // 主表保存累计总额度和日/月配置；当前剩余额度以日/月明细为唯一事实来源。
        int configuredMonthCount = defaultCount(account.getMonthCount());
        int configuredDayCount = defaultCount(account.getDayCount());
        account.setMonthCount(monthAccount == null ? configuredMonthCount : defaultCount(monthAccount.getMonthCount()));
        account.setMonthCountSurplus(monthAccount == null
                ? configuredMonthCount : defaultCount(monthAccount.getMonthCountSurplus()));
        account.setDayCount(dayAccount == null ? configuredDayCount : defaultCount(dayAccount.getDayCount()));
        account.setDayCountSurplus(dayAccount == null
                ? configuredDayCount : defaultCount(dayAccount.getDayCountSurplus()));
        return account;
    }

    private int defaultCount(Integer value) {
        return value == null ? 0 : value;
    }

    @Override
    public Integer queryRaffleActivityAccountPartakeCount(Long activityId, String userId) {
        return activityRepository.queryRaffleActivityAccountPartakeCount(activityId, userId);
    }

    /**
     * 将超过一个月的「待支付」订单批量置为过期。
     *
     * @param beforeTime 过期临界时间
     * @return 影响行数
     */
    @Override
    public int updateOrderExpired(Date beforeTime) {
        return activityRepository.updateOrderExpired(beforeTime);
    }

}
