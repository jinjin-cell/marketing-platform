package cn.qijiv.domain.activity.service.armory;

/**
 * 活动装配接口
 */
public interface IActivityArmory {

    /**
     * 预热活动SKU库存
     *
     * @param sku 活动商品SKU
     * @return 预热结果
     */
    boolean assembleActivitySku(Long sku);

}
