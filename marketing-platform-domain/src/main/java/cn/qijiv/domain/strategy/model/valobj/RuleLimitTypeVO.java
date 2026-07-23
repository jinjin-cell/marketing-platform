package cn.qijiv.domain.strategy.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;

/** 规则树连线的限定关系。 */
@Getter
@AllArgsConstructor
public enum RuleLimitTypeVO {

    EQUAL(1, "等于"),
    GT(2, "大于"),
    LT(3, "小于"),
    GE(4, "大于等于"),
    LE(5, "小于等于"),
    ENUM(6, "枚举"),
    ;

    private final Integer code;
    private final String info;

    /**
     * 判断规则节点的执行结果是否命中当前连线。
     *
     * @param actualValue   节点实际返回值
     * @param expectedValue 连线配置值
     * @return 是否命中
     */
    public boolean matches(String actualValue, String expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        // 连线只负责判断节点结果是否满足跳转条件，不参与具体业务规则计算。
        switch (this) {
            case EQUAL:
                return actualValue.equals(expectedValue);
            case GT:
                return number(actualValue).compareTo(number(expectedValue)) > 0;
            case LT:
                return number(actualValue).compareTo(number(expectedValue)) < 0;
            case GE:
                return number(actualValue).compareTo(number(expectedValue)) >= 0;
            case LE:
                return number(actualValue).compareTo(number(expectedValue)) <= 0;
            case ENUM:
                return Arrays.stream(expectedValue.split(","))
                        .map(String::trim)
                        .anyMatch(actualValue::equals);
            default:
                throw new IllegalStateException("不支持的规则树限定类型: " + this);
        }
    }

    private BigDecimal number(String value) {
        try {
            // 使用 BigDecimal 避免浮点数比较产生精度误差。
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("规则树数值比较配置非法，value: " + value, ex);
        }
    }

}
