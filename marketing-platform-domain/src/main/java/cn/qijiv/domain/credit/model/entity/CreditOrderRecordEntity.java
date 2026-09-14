package cn.qijiv.domain.credit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 积分流水记录（只读查询模型）
 *
 * <p>与 {@link CreditOrderEntity} 的区别：写模型用枚举表达交易名称/类型，
 * 而库里 `trade_name` 存的是展示名、`trade_type` 存的是编码，读模型按原样承载，避免反向枚举转换丢数据。
 *
 * @author qijiv
 * @since 2026/09/14
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditOrderRecordEntity implements Serializable {

    /** 订单ID */
    private String orderId;
    /** 交易名称（展示名，如 行为返利、兑换抽奖） */
    private String tradeName;
    /** 交易类型编码；forward-正向入账、reverse-逆向扣减 */
    private String tradeType;
    /** 交易金额 */
    private BigDecimal tradeAmount;
    /** 业务防重ID */
    private String outBusinessNo;
    /** 交易时间 */
    private Date createTime;

}
