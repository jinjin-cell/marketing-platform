package cn.qijiv.domain.strategy.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 抽奖规则检查结果。 */
@Getter
@AllArgsConstructor
public enum RuleLogicCheckTypeVO {

    /** 规则检查通过，允许放行 */
    ALLOW("0000", "规则放行"),
    /** 规则拦截接管，由规则返回结果 */
    TAKE_OVER("0001", "规则接管"),
    ;

    /** 检查结果编码 */
    private final String code;
    /** 检查结果描述 */
    private final String info;
}
