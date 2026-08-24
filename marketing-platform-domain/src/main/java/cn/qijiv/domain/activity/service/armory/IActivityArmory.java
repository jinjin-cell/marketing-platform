package cn.qijiv.domain.activity.service.armory;

/**
 * 活动装配接口
 */
public interface IActivityArmory {

    /**
     * 根据活动ID预热活动SKU库存
     *
     * @param activityId 活动ID
     * @return 预热结果
     */
    boolean assembleActivitySkuByActivityId(Long activityId);
}