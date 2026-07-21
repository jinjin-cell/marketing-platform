package cn.qijiv.domain.strategy.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 抽奖规则检查结果。 */
@Getter
@AllArgsConstructor
public enum RuleLogicCheckTypeVO {

    ALLOW("0000", "规则放行"),
    TAKE_OVER("0001", "规则接管"),
    ;

    private final String code;
    private final String info;
}
