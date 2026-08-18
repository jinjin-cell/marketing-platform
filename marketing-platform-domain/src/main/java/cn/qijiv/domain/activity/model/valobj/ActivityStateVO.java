package cn.qijiv.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 活动状态
 * 
 * @author qijiv
 * @since 2026/7/18
 */
@Getter
@AllArgsConstructor
public enum ActivityStateVO {

    /** 创建 */
    create("create", "创建"),
    /** 开启 */
    open("open", "开启"),
    /** 关闭 */
    close("close", "关闭"),
    ;

    /** 状态编码 */
    private final String code;
    /** 状态描述 */
    private final String desc;

}

