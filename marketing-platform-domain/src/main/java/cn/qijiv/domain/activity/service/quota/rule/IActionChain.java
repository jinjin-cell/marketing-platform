package cn.qijiv.domain.activity.service.quota.rule;

import cn.qijiv.domain.activity.model.entity.ActivityCountEntity;
import cn.qijiv.domain.activity.model.entity.ActivityEntity;
import cn.qijiv.domain.activity.model.entity.ActivitySkuEntity;

/**
 * 活动操作链接口
 *
 * @author qijiv
 * @since 2026/7/18
 */
public interface IActionChain extends IActionChainArmory {

    /**
     * 执行责任链动作
     *
     * @param activitySkuEntity 活动SKU实体
     * @param activityEntity 活动实体
     * @param activityCountEntity 活动次数配置实体
     * @return 执行结果
     */
    boolean action(ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity);

}

