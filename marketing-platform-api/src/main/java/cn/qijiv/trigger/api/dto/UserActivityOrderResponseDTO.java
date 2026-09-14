package cn.qijiv.trigger.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户活动订单响应DTO（兑换/充值记录）
 *
 * @author qijiv
 * @since 2026/09/14
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserActivityOrderResponseDTO implements Serializable {

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 商品SKU
     */
    private Long sku;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 充值总次数
     */
    private Integer totalCount;

    /**
     * 充值日次数
     */
    private Integer dayCount;

    /**
     * 充值月次数
     */
    private Integer monthCount;

    /**
     * 支付金额（积分）
     */
    private BigDecimal payAmount;

    /**
     * 订单状态编码；wait_pay-待支付、completed-完成、expired-已过期
     */
    private String state;

    /**
     * 订单状态说明
     */
    private String stateDesc;

    /**
     * 下单时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date orderTime;

}
