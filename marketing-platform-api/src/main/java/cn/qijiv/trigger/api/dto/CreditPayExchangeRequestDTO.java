package cn.qijiv.trigger.api.dto;

import lombok.Data;

import java.io.Serializable;

/** 积分兑换活动商品请求。 */
@Data
public class CreditPayExchangeRequestDTO implements Serializable {

    /** 用户ID。 */
    private String userId;
    /** 活动商品SKU。 */
    private Long sku;
    /** 调用方生成的幂等业务号。 */
    private String outBusinessNo;
}
