package cn.qijiv.infrastructure.persistent.repository;

import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.infrastructure.persistent.dao.IStrategyAwardDao;
import cn.qijiv.infrastructure.persistent.dao.IStrategyDao;
import cn.qijiv.infrastructure.persistent.dao.IStrategyRuleDao;
import cn.qijiv.infrastructure.persistent.po.StrategyAwardPO;
import cn.qijiv.infrastructure.persistent.po.StrategyPO;
import cn.qijiv.infrastructure.persistent.po.StrategyRulePO;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import cn.qijiv.types.common.Constants;


/**
 * 抽奖策略奖品持久化仓库
 *
 * @author jinlujia
 * @date 2026/07/18
 */
@Repository
public class StrategyRespository implements IStrategyRepository {

    @Resource
    private IStrategyAwardDao strategyAwardDao;

    @Resource
    private IStrategyDao strategyDao;

    @Resource
    private IStrategyRuleDao strategyRuleDao;

    @Resource
    private IRedisService redisService;

    @Override
    public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
        // 1. 先从 Redis 缓存中查询
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_KEY + strategyId;
        List<StrategyAwardEntity> cachedList = redisService.getValue(cacheKey);
        if (cachedList != null && !cachedList.isEmpty()) {
            return cachedList;
        }

        // 2. 缓存未命中，从数据库查询
        List<StrategyAwardPO> strategyAwardPOList = strategyAwardDao.queryStrategyAwardListByStrategyId(strategyId);
        List<StrategyAwardEntity> entityList = strategyAwardPOList.stream()
                .map(strategyAwardPO -> StrategyAwardEntity.builder()
                        .strategyId(strategyAwardPO.getStrategyId())
                        .awardId(strategyAwardPO.getAwardId())
                        .awardCount(strategyAwardPO.getAwardCount())
                        .awardCountSurplus(strategyAwardPO.getAwardCountSurplus())
                        .awardRate(strategyAwardPO.getAwardRate())
                        .build())
                .collect(Collectors.toList());

        // 3. 将数据库结果写入 Redis 缓存
        redisService.setValue(cacheKey, entityList);

        return entityList;
    }

    /**
     * 存储策略奖品概率查找表
     *
     * @param strategyId 策略ID
     * @param rateTable  概率查找表，列表下标为随机值，元素为奖品ID
     */
    @Override
    public void storeStrategyRateTable(String strategyId, List<Integer> rateTable) {
        // 普通策略和权重策略共用前缀，通过业务key区分不同概率表。
        String cacheKey = Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + strategyId;
        redisService.setList(cacheKey, rateTable);
    }

    /**
     * 查询策略概率表的槽位数量，作为随机数上界
     *
     * @param strategyKey 策略表业务key
     * @return 概率表槽位数量
     */
    @Override
    public Integer queryStrategyRateTableSize(String strategyKey) {
        String cacheKey = Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + strategyKey;
        int size = redisService.getListSize(cacheKey);
        return size > 0 ? size : null;
    }

    /**
     * 按随机值查询策略奖品 ID
     *
     * @param strategyKey 策略表业务key
     * @param randomValue 概率表下标
     * @return 奖品ID
     */
    @Override
    public Integer queryStrategyAwardId(String strategyKey, Integer randomValue) {
        String cacheKey = Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + strategyKey;
        return redisService.getListValue(cacheKey, randomValue);
    }

    /**
     * 查询策略实体，包含规则模型
     *
     * @param strategyId 策略ID
     * @return 策略实体
     */
    @Override
    public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId) {
        // rule_models 决定本次抽奖实际执行的规则，直接读取数据库以避免永久缓存旧配置。
        StrategyPO strategyPO = strategyDao.queryStrategyByStrategyId(strategyId);
        if (strategyPO == null) {
            return null;
        }
        StrategyEntity entity = StrategyEntity.builder()
                .strategyId(strategyPO.getStrategyId())
                .strategyDesc(strategyPO.getStrategyDesc())
                .ruleModels(strategyPO.getRuleModels())
                .build();
        return entity;
    }

    @Override
    public StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel) {
        // 按策略ID和规则模型精确查询策略级规则，例如 rule_weight。
        StrategyRulePO strategyRulePO = strategyRuleDao.queryStrategyRule(strategyId, ruleModel);
        if (strategyRulePO == null) {
            return null;
        }
        // 基础设施PO转换为领域实体，规则字符串的解析由领域实体负责。
        return StrategyRuleEntity.builder()
                .strategyId(Long.valueOf(strategyRulePO.getStrategyId()))
                .awardId(strategyRulePO.getAwardId())
                .ruleType(strategyRulePO.getRuleType())
                .ruleModel(strategyRulePO.getRuleModel())
                .ruleValue(strategyRulePO.getRuleValue())
                .ruleDesc(strategyRulePO.getRuleDesc())
                .build();
    }
}
