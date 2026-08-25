package cn.qijiv.domain.strategy.repository;

import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeVO;
import cn.qijiv.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.qijiv.domain.strategy.model.valobj.StrategyAwardStockKeyVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 抽奖策略仓库
 *
 * @author jinlujia
 * @since 2026-07-18
 */
public interface IStrategyRepository {

    /**
     * 查询抽奖策略奖品列表
     *
     * @param strategyId 抽奖策略ID
     * @return 抽奖策略奖品列表
     */
    List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId);

    /** 查询策略中的指定奖品，不存在时返回null。 */
    StrategyAwardEntity queryStrategyAwardEntity(Long strategyId, Integer awardId);

    /**
     * 存储策略奖品概率查找表
     *
     * @param strategyId 策略表业务key，可以是策略ID或策略ID+权重配置
     * @param rateTable  概率查找表，列表下标为随机值，元素为奖品ID
     */
    void storeStrategyRateTable(String strategyId, List<Integer> rateTable);

    /**
     * 查询策略概率表的槽位数量，作为随机数上界
     *
     * @param strategyKey 策略表业务key
     * @return 概率表槽位数量
     */
    Integer queryStrategyRateTableSize(String strategyKey);

    /**
     * 按随机值查询策略奖品 ID
     *
     * @param strategyKey 策略表业务key
     * @param randomValue 概率表下标
     * @return 奖品ID
     */
    Integer queryStrategyAwardId(String strategyKey, Integer randomValue);

    /**
     * 查询策略实体
     *
     * @param strategyId 策略ID
     * @return 策略实体
     */
    StrategyEntity queryStrategyEntityByStrategyId(Long strategyId);

    /**
     * 查询策略规则
     *
     * @param strategyId 策略ID
     * @param ruleModel  规则模型
     * @return 策略规则实体
     */
    StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel);

    /**
     * 查询奖品绑定的规则树模型。
     *
     * @param strategyId 策略ID
     * @param awardId    奖品ID
     * @return 奖品规则树配置；奖品不存在时返回null
     */
    StrategyAwardRuleModelVO queryStrategyAwardRuleModelVO(Long strategyId, Integer awardId);

    /**
     * 从规则树、节点、连线三张表装配一棵完整规则树。
     *
     * @param treeId 规则树业务ID
     * @return 完整规则树；树根不存在时返回null
     */
    RuleTreeVO queryRuleTreeVOByTreeId(String treeId);

    /**
     * 批量查询规则树的次数解锁配置
     *
     * @param treeIds 规则树ID列表
     * @return 规则树ID -> 解锁次数
     */
    Map<String, Integer> queryAwardRuleLockCount(List<String> treeIds);


    /**
     * 缓存抽奖奖品库存
     *
     * @param cacheKey 缓存key
     * @param awardCount 奖品库存
     */
    void cacheStrategyAwardCount(String cacheKey, Integer awardCount);

    /**
     * 原子扣减奖品库存。
     *
     * @return true-扣减成功，false-库存不足或奖品不存在
     * @param cacheKey 缓存key
     */
    Boolean subtractionAwardStock(String cacheKey);

    /**
     * 原子扣减奖品库存，并按活动结束时间给库存锁设置缓存有效期。
     *
     * @param cacheKey    缓存key
     * @param endDateTime 活动结束时间，可为空；为空时不设置锁缓存有效期
     * @return true-扣减成功，false-库存不足或奖品不存在
     */
    Boolean subtractionAwardStock(String cacheKey, Date endDateTime);

    /**
     * 发送抽奖奖品库存扣减消息到队列
     *
     * @param strategyAwardStockKeyVO 抽奖奖品库存扣减消息
     */
    void awardStockConsumeSendQueue(StrategyAwardStockKeyVO strategyAwardStockKeyVO);

    /** 获取一条已经到期的库存扣减消息。 */
    StrategyAwardStockKeyVO takeQueueValue() throws InterruptedException;

    /** 将一次成功的 Redis 库存扣减同步到数据库。 */
    void updateStrategyAwardStock(Long strategyId, Integer awardId);

    /** 根据活动ID查询策略ID。 */
    Long queryStrategyIdByActivityId(Long activityId);

    /** 查询用户今日已抽奖次数。 */
    Integer queryTodayUserRaffleCount(String userId, Long strategyId);
}
