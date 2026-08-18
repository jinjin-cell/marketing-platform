package cn.qijiv.domain.activity.service.quota;

import cn.qijiv.domain.activity.model.entity.ActivityCountEntity;
import cn.qijiv.domain.activity.model.entity.ActivityEntity;
import cn.qijiv.domain.activity.model.entity.ActivitySkuEntity;
import cn.qijiv.domain.activity.repository.IActivityRepository;
import cn.qijiv.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;

/**
 * 抽奖活动支持类
 *
 * @author qijiv
 * @since 2026/7/18
 */
public class RaffleActivityAccountQuotaSupport {

    /** 默认活动责任链工厂 */
    protected DefaultActivityChainFactory defaultActivityChainFactory;

    /** 活动仓库 */
    protected IActivityRepository activityRepository;

    /**
     * 构造方法注入活动仓库与责任链工厂
     *
     * @param activityRepository 活动仓库
     * @param defaultActivityChainFactory 默认活动责任链工厂
     */
    public RaffleActivityAccountQuotaSupport(IActivityRepository activityRepository, DefaultActivityChainFactory defaultActivityChainFactory) {
        this.activityRepository = activityRepository;
        this.defaultActivityChainFactory = defaultActivityChainFactory;
    }

    /**
     * 查询活动SKU信息
     *
     * @param sku 活动商品SKU
     * @return 活动SKU实体
     */
    public ActivitySkuEntity queryActivitySku(Long sku) {
        return activityRepository.queryActivitySku(sku);
    }

    /**
     * 查询抽奖活动信息
     *
     * @param activityId 活动ID
     * @return 活动实体
     */
    public ActivityEntity queryRaffleActivityByActivityId(Long activityId) {
        return activityRepository.queryRaffleActivityByActivityId(activityId);
    }

    /**
     * 查询抽奖活动次数配置信息
     *
     * @param activityCountId 活动次数配置ID
     * @return 活动次数配置实体
     */
    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
        return activityRepository.queryRaffleActivityCountByActivityCountId(activityCountId);
    }

}
