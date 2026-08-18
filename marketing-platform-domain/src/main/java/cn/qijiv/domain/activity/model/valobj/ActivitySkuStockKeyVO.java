package cn.qijiv.domain.activity.model.valobj;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 活动SKU库存扣减键值对象，用于在库存延迟队列中标识需要扣减库存的SKU及其所属活动
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivitySkuStockKeyVO {

    /** 商品sku */
    private Long sku;
    /** 活动ID */
    private Long activityId;

}

