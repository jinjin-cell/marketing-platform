package cn.qijiv.infrastructure.dao.po;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 抽奖活动订单持久化对象
 *
 * @author jinlujia
 * @since 2026-07-27
 */
@Data
public class RaffleActivityOrderPO {

    /**
     * 自增ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 商品sku
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
     * 抽奖策略ID
     */
    private Long strategyId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 外部业务单号
     */
    private String outBusinessNo;

    /**
     * 下单时间
     */
    private Date orderTime;

    /**
     * 总次数
     */
    private Integer totalCount;

    /**
     * 日次数
     */
    private Integer dayCount;

    /**
     * 月次数
     */
    private Integer monthCount;

    /**
     * 支付金额[积分]
     */
    private BigDecimal payAmount;

    /**
     * 订单状态（complete）
     */
    private String state;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
