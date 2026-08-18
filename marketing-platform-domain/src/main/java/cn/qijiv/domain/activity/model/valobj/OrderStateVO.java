package cn.qijiv.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态
 * 
 * @author qijiv
 * @since 2026/7/18
 */
@Getter
@AllArgsConstructor
public enum OrderStateVO {

    /** 已完成 */
    completed("completed", "完成");

    /** 状态编码 */
    private final String code;
    /** 状态描述 */
    private final String desc;

}

