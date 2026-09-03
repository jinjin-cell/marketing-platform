package cn.qijiv.types.common;

/**
 * 通用常量类，存放基础分隔符等公共常量
 */
public class Constants {

    /** 逗号分隔符 */
    public final static String SPLIT = ",";
    /** 冒号分隔符 */
    public final static String COLON = ":";
    /** 空格分隔符 */
    public final static String SPACE = " ";
    /** 下划线分隔符 */
    public final static String UNDERLINE = "_";
    /** 黑名单积分随机奖励范围 */
    public final static String CREDIT_BLACKLIST_RANGE = "0.01,1";

    /**
     * Redis Key 常量类
     */
    public static class RedisKey {

    /** 活动信息缓存 Key 前缀，后接活动 ID */
    public static String ACTIVITY_KEY = "big_market_activity_key_";

    /** 活动 SKU 缓存 Key 前缀，后接 SKU ID */
    public static String ACTIVITY_SKU_KEY = "big_market_activity_sku_key_";

    /** 活动次数额度缓存 Key 前缀，后接活动 ID */
    public static String ACTIVITY_COUNT_KEY = "big_market_activity_count_key_";

    /** 活动订单业务号布隆过滤器；版本变化时使用新 key 重新装载。 */
    public static String ACTIVITY_ORDER_BUSINESS_NO_BLOOM_FILTER = "big_market_activity_order_business_no_bloom_filter_v1";

    /** 策略奖品列表缓存 Key 前缀，后接策略 ID */
    public static String STRATEGY_AWARD_LIST_KEY = "big_market_strategy_award_list_key_";

    /** 策略权重概率表缓存 Key 前缀，后接策略 ID */
    public static String STRATEGY_RATE_TABLE_KEY = "big_market_strategy_rate_table_key_";

    /** 策略信息缓存 Key 前缀，后接策略 ID */
    public static String STRATEGY_KEY = "big_market_strategy_key_";

    /** 策略权重概率范围缓存 Key 前缀，后接策略 ID */
    public static String STRATEGY_RATE_RANGE_KEY = "big_market_strategy_rate_range_key_";

    /** 规则树缓存，后接缓存版本和treeId。 */
    public static String RULE_TREE_KEY = "big_market_rule_tree_key_";

    /** 规则树 VO 缓存 Key 前缀，后接缓存版本与 treeId */
    public static String RULE_TREE_VO_KEY = "big_market_rule_tree_vo_key_";

    /** 策略奖品库存缓存 Key 前缀，后接策略 ID */
    public static String STRATEGY_AWARD_COUNT_KEY = "big_market_strategy_award_count_key_";

    /** 策略奖品库存查询 Key */
    public static String STRATEGY_AWARD_COUNT_QUERY_KEY = "strategy_award_count_query_key";

    /** 活动 SKU 库存查询 Key */
    public static String ACTIVITY_SKU_COUNT_QUERY_KEY = "activity_sku_count_query_key";
    
    /** 活动 SKU 库存数量缓存 Key 前缀，后接 SKU ID */
    public static String ACTIVITY_SKU_STOCK_COUNT_KEY = "activity_sku_stock_count_key_";

    /** 活动 SKU 库存清理缓存 Key 前缀，后接 SKU ID */
    public static String ACTIVITY_SKU_COUNT_CLEAR_KEY = "activity_sku_count_clear_key_";

    /** 活动账户操作分布式锁 Key 前缀，后接 userId_activityId */
    public static String ACTIVITY_ACCOUNT_LOCK = "big_market_activity_account_lock_";

    /** 奖品配置缓存 Key 前缀，后接奖品 ID */
    public static String AWARD_CONFIG_KEY = "big_market_award_config_key_";

    /** 奖品 Key 缓存前缀，后接奖品 ID */
    public static String AWARD_KEY = "big_market_award_key_";


}

}
