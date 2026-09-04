package cn.qijiv.domain.activity.model.entity;

import cn.qijiv.domain.activity.model.valobj.OrderTradeTypeVO;
import lombok.Data;


/**
 * sku 账户充值订单实体对象
 * @author qijiv
 * @date 2026/7/18
 */
@Data
public class SkuRechargeEntity {

    /** 用户ID */
    private String userId;
    /** 商品SKU - activity + activity count */
    private Long sku;
    /** 幂等业务单号，外部谁充值谁透传，这样来保证幂等（多次调用也能确保结果唯一，不会多次充值）。 */
    private String outBusinessNo;

    /** 订单交易类型 */
    private OrderTradeTypeVO orderTradeType = OrderTradeTypeVO.rebate_no_pay_trade;
}

