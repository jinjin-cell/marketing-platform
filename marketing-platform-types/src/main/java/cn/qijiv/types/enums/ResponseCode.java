package cn.qijiv.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 统一响应状态码枚举
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    /** 调用成功 */
    SUCCESS("0000", "调用成功"),
    /** 调用失败 */
    UN_ERROR("0001", "调用失败"),
    /** 非法参数 */
    ILLEGAL_PARAMETER("0002", "非法参数"),
    /** 唯一索引冲突 */
    INDEX_DUP("0003", "唯一索引冲突"),
    /** 策略规则权重配置异常 */
    STRATEGY_RULE_WEIGHT_IS_NULL("ERR_BIZ_001", "业务异常，策略规则中 rule_weight 权重规则已适用但未配置"),
    /** 抽奖策略配置未装配 */
    UN_ASSEMBLED_STRATEGY_ARMORY("ERR_BIZ_002", "抽奖策略配置未装配，请通过IStrategyArmory完成装配"),
    /** 活动未开启 */
    ACTIVITY_STATE_ERROR("ERR_BIZ_003", "活动未开启（非open状态）"),
    /** 非活动日期范围 */
    ACTIVITY_DATE_ERROR("ERR_BIZ_004", "非活动日期范围"),
    /** 活动库存不足 */
    ACTIVITY_SKU_STOCK_ERROR("ERR_BIZ_005", "活动库存不足"),
    /** 账户总额度不足 */
    ACCOUNT_QUOTA_ERROR("ERR_BIZ_006","账户总额度不足"),
    /** 账户月额度不足 */
    ACCOUNT_MONTH_QUOTA_ERROR("ERR_BIZ_007","账户月额度不足"),
    /** 账户日额度不足 */
    ACCOUNT_DAY_QUOTA_ERROR("ERR_BIZ_008","账户日额度不足"),
    /** 用户抽奖单已使用过，不可重复抽奖 */
    ACTIVITY_ORDER_ERROR("ERR_BIZ_009", "用户抽奖单已使用过，不可重复抽奖"),
    ;


    /** 状态码 */
    private String code;
    /** 状态说明 */
    private String info;

}
