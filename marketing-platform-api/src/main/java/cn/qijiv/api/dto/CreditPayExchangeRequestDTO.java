package cn.qijiv.api.dto;

import lombok.Data;

/** 积分兑换活动商品请求。 */
@Data
public class CreditPayExchangeRequestDTO {

    /** 用户ID。 */
    private String userId;
    /** 活动商品SKU。 */
    private Long sku;
    /** 调用方生成的幂等业务号。 */
    private String outBusinessNo;
}
