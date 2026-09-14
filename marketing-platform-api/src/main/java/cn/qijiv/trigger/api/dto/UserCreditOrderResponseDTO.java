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
 * 用户积分流水响应DTO（积分明细）
 *
 * @author qijiv
 * @since 2026/09/14
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCreditOrderResponseDTO implements Serializable {

    /**
     * 积分订单ID
     */
    private String orderId;

    /**
     * 交易名称；如 行为返利、兑换抽奖
     */
    private String tradeName;

    /**
     * 交易类型编码；forward-正向入账、reverse-逆向扣减
     */
    private String tradeType;

    /**
     * 交易类型说明
     */
    private String tradeTypeDesc;

    /**
     * 交易金额（带符号）
     */
    private BigDecimal tradeAmount;

    /**
     * 业务防重ID（返利、行为等唯一标识）
     */
    private String outBusinessNo;

    /**
     * 交易时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

}
