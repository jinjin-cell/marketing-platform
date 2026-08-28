package cn.qijiv.domain.rebate.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 行为返利类型。 */
@Getter
@AllArgsConstructor
public enum RebateTypeVO {

    SKU("sku", "活动库存充值商品"),
    INTEGRAL("integral", "用户活动积分");

    private final String code;
    private final String info;
}
