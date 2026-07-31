package cn.qijiv.domain.activity.model.valobj;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

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

