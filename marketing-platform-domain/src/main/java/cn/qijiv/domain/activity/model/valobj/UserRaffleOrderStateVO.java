package cn.qijiv.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户抽奖订单状态枚举
 *
 * @author qijiv
 * @since 2026/7/18
 */
@Getter
@AllArgsConstructor
public enum UserRaffleOrderStateVO {

    /** 创建 */
    create("create", "创建"),
    /** 已使用 */
    used("used", "已使用"),
    /** 已作废 */
    cancel("cancel", "已作废"),
    ;

    /** 状态编码 */
    private final String code;
    /** 状态描述 */
    private final String desc;

}

