package cn.qijiv.domain.credit.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易名称枚举值
 * @author qijiv
 * @since 2026/9/3
 */
@Getter
@AllArgsConstructor
public enum TradeNameVO {

    REBATE("rebate", "行为返利"),
    CONVERT_SKU("convert_sku", "兑换抽奖"),

    ;

    private final String code;
    private final String name;

}

