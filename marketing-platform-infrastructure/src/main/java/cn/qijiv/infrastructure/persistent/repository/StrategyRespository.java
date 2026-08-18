package cn.qijiv.infrastructure.persistent.repository;

import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.infrastructure.persistent.dao.IStrategyAwardDao;
import cn.qijiv.infrastructure.persistent.dao.IStrategyDao;
import cn.qijiv.infrastructure.persistent.dao.IStrategyRuleDao;
import cn.qijiv.infrastructure.persistent.dao.IRuleTreeDao;
import cn.qijiv.infrastructure.persistent.dao.IRuleTreeNodeDao;
import cn.qijiv.infrastructure.persistent.dao.IRuleTreeNodeLineDao;
import cn.qijiv.infrastructure.persistent.po.RuleTreeNodeLinePO;
import cn.qijiv.infrastructure.persistent.po.RuleTreeNodePO;
import cn.qijiv.infrastructure.persistent.po.RuleTreePO;
import cn.qijiv.infrastructure.persistent.po.StrategyAwardPO;
import cn.qijiv.infrastructure.persistent.po.StrategyPO;
import cn.qijiv.infrastructure.persistent.po.StrategyRulePO;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.model.valobj.RuleLimitTypeVO;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeNodeLineVO;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeNodeVO;
import cn.qijiv.domain.strategy.model.valobj.RuleTreeVO;
import cn.qijiv.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.qijiv.domain.strategy.model.valobj.StrategyAwardStockKeyVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import cn.qijiv.types.common.Constants;
import lombok.extern.slf4j.Slf4j;



/**
 * 抽奖策略奖品持久化仓库
 *
 * @author jinlujia
 * @since 2026-07-18
 */
@Slf4j
@Repository
public class StrategyRespository implements IStrategyRepository {

    private static final long STRATEGY_AWARD_CACHE_TTL_MINUTES = 10L;  // 抽奖策略奖品缓存过期时间
    private static final long STRATEGY_RATE_TABLE_CACHE_TTL_MINUTES = 30L;  // 抽奖策略奖品率表缓存过期时间
    private static final long RULE_TREE_CACHE_TTL_MINUTES = 30L;   // 规则树缓存过期时间

    /** 策略奖品 DAO */
    @Resource
    private IStrategyAwardDao strategyAwardDao;

    /** 策略 DAO */
    @Resource
    private IStrategyDao strategyDao;

    /** 策略规则 DAO */
    @Resource
    private IStrategyRuleDao strategyRuleDao;

    /** 规则树 DAO */
    @Resource
    private IRuleTreeDao ruleTreeDao;

    /** 规则树节点 DAO */
    @Resource
    private IRuleTreeNodeDao ruleTreeNodeDao;

    /** 规则树节点连线 DAO */
    @Resource
    private IRuleTreeNodeLineDao ruleTreeNodeLineDao;

    /** Redis 服务 */
    @Resource
    private IRedisService redisService;

    /**
     * 查询策略奖品列表，优先从缓存获取
     *
     * @param strategyId 策略ID
     * @return 策略奖品实体列表
     */
    @Override
    public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
        // 1. 先从 Redis 缓存中查询
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_LIST_KEY + strategyId;
        List<StrategyAwardEntity> cachedList = redisService.getValue(cacheKey);
        if (cachedList != null && !cachedList.isEmpty()) {
            return cachedList;
        }

        // 2. 缓存未命中，从数据库查询
        List<StrategyAwardPO> strategyAwardPOList =
                strategyAwardDao.queryStrategyAwardListByStrategyId(strategyId);
        if (strategyAwardPOList == null || strategyAwardPOList.isEmpty()) {
            return Collections.emptyList();
        }
        List<StrategyAwardEntity> entityList = strategyAwardPOList.stream()
                .map(strategyAwardPO -> StrategyAwardEntity.builder()
                        .strategyId(strategyAwardPO.getStrategyId())
                        .awardId(strategyAwardPO.getAwardId())
                        .awardTitle(strategyAwardPO.getAwardTitle())
                        .awardSubTitle(strategyAwardPO.getAwardSubtitle())
                        .awardCount(strategyAwardPO.getAwardCount())
                        .awardCountSurplus(strategyAwardPO.getAwardCountSurplus())
                        .awardRate(strategyAwardPO.getAwardRate())
                        .sort(strategyAwardPO.getSort())
                        .build())
                .collect(Collectors.toList());

        // 3. 奖品规则可能动态调整，缓存定期过期以避免长期使用旧 ruleModels。
        redisService.setValue(
                cacheKey,
                entityList,
                STRATEGY_AWARD_CACHE_TTL_MINUTES,
                TimeUnit.MINUTES);

        return entityList;
    }

    /**
     * 按策略ID与奖品ID查询单个策略奖品
     *
     * @param strategyId 策略ID
     * @param awardId    奖品ID
     * @return 策略奖品实体，不存在时返回 null
     */
    @Override
    public StrategyAwardEntity queryStrategyAwardEntity(Long strategyId, Integer awardId) {
        if (strategyId == null || awardId == null) {
            return null;
        }
        List<StrategyAwardEntity> strategyAwardList = queryStrategyAwardList(strategyId);
        for (StrategyAwardEntity strategyAward : strategyAwardList) {
            if (strategyAward != null && awardId.equals(strategyAward.getAwardId())) {
                return strategyAward;
            }
        }
        return null;
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
        redisService.setList(
                cacheKey,
                rateTable,
                STRATEGY_RATE_TABLE_CACHE_TTL_MINUTES,
                TimeUnit.MINUTES);
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
        return StrategyEntity.builder()
                .strategyId(strategyPO.getStrategyId())
                .strategyDesc(strategyPO.getStrategyDesc())
                .ruleModels(strategyPO.getRuleModels())
                .build();
    }

    /**
     * 查询策略级规则
     *
     * @param strategyId 策略ID
     * @param ruleModel  规则模型
     * @return 策略规则实体，不存在时返回 null
     */
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

    /**
     * 查询奖品级规则
     *
     * @param strategyId 策略ID
     * @param awardId    奖品ID
     * @param ruleModel  规则模型
     * @return 策略规则实体，不存在时返回 null
     */
    @Override
    public StrategyRuleEntity queryStrategyAwardRule(Long strategyId, Integer awardId, String ruleModel) {
        StrategyRulePO strategyRulePO = strategyRuleDao.queryStrategyAwardRule(strategyId, awardId, ruleModel);
        if (strategyRulePO == null) {
            return null;
        }
        return StrategyRuleEntity.builder()
                .strategyId(Long.valueOf(strategyRulePO.getStrategyId()))
                .awardId(strategyRulePO.getAwardId())
                .ruleType(strategyRulePO.getRuleType())
                .ruleModel(strategyRulePO.getRuleModel())
                .ruleValue(strategyRulePO.getRuleValue())
                .ruleDesc(strategyRulePO.getRuleDesc())
                .build();
    }

    /**
     * 查询策略奖品规则模型配置
     *
     * @param strategyId 策略ID
     * @param awardId    奖品ID
     * @return 策略奖品规则模型值对象，不存在时返回 null
     */
    @Override
    public StrategyAwardRuleModelVO queryStrategyAwardRuleModelVO(
            Long strategyId, Integer awardId) {
        StrategyAwardPO strategyAwardPO =
                strategyAwardDao.queryStrategyAwardRuleModel(strategyId, awardId);
        if (strategyAwardPO == null) {
            return null;
        }
        return StrategyAwardRuleModelVO.builder()
                .ruleModels(strategyAwardPO.getRuleModels())
                .build();
    }

    /**
     * 查询规则树，优先从缓存获取，未命中时从数据库装配
     *
     * @param treeId 规则树ID
     * @return 规则树值对象，不存在时返回 null
     */
    @Override
    public RuleTreeVO queryRuleTreeVOByTreeId(String treeId) {
        if (treeId == null || treeId.trim().isEmpty()) {
            throw new IllegalArgumentException("规则树ID不能为空");
        }

        String normalizedTreeId = treeId.trim();
        String cacheKey = Constants.RedisKey.RULE_TREE_KEY + normalizedTreeId;

        // 1. 先查询 Redis，命中后不再访问规则树三张表。
        RuleTreeVO cachedRuleTree = redisService.getValue(cacheKey);
        if (cachedRuleTree != null) {
            return cachedRuleTree;
        }

        // 2. 缓存未命中，从数据库装配完整规则树。
        RuleTreeVO ruleTreeVO = queryRuleTreeFromDatabase(normalizedTreeId);
        if (ruleTreeVO == null) {
            return null;
        }

        // 3. 将完整规则树写入 Redis，并设置固定过期时间，避免配置长期不刷新。
        redisService.setValue(
                cacheKey,
                ruleTreeVO,
                RULE_TREE_CACHE_TTL_MINUTES,
                TimeUnit.MINUTES);
        return ruleTreeVO;
    }

    /** 从规则树、节点和连线三张表装配领域对象。 */
    private RuleTreeVO queryRuleTreeFromDatabase(String treeId) {
        // 1. 查询树根。树根不存在时返回 null，由领域服务给出包含奖品信息的异常。
        RuleTreePO ruleTreePO = ruleTreeDao.queryRuleTreeByTreeId(treeId);
        if (ruleTreePO == null) {
            return null;
        }

        // 2. 一次性查询该树的全部节点和连线，避免执行过程中频繁访问数据库。
        List<RuleTreeNodePO> nodePOList = ruleTreeNodeDao.queryRuleTreeNodeListByTreeId(treeId);
        List<RuleTreeNodeLinePO> linePOList =
                ruleTreeNodeLineDao.queryRuleTreeNodeLineListByTreeId(treeId);
        if (nodePOList == null || nodePOList.isEmpty()) {
            throw new IllegalStateException("规则树没有配置节点，treeId: " + treeId);
        }

        // 3. tree node line 转换成Map结构
        Map<String, List<RuleTreeNodeLineVO>> lineMap = new LinkedHashMap<>();
        if (linePOList != null) {
            for (RuleTreeNodeLinePO linePO : linePOList) {
                RuleTreeNodeLineVO lineVO = convertLine(linePO);
                lineMap.computeIfAbsent(lineVO.getRuleNodeFrom(), key -> new ArrayList<>())
                        .add(lineVO);
            }
        }

        // 4. tree node 转换成Map结构
        Map<String, RuleTreeNodeVO> nodeMap = new LinkedHashMap<>();
        for (RuleTreeNodePO nodePO : nodePOList) {
            RuleTreeNodeVO old = nodeMap.put(nodePO.getRuleKey(), RuleTreeNodeVO.builder()
                    .treeId(nodePO.getTreeId())
                    .ruleKey(nodePO.getRuleKey())
                    .ruleDesc(nodePO.getRuleDesc())
                    .ruleValue(nodePO.getRuleValue())
                    .treeNodeLineVOList(lineMap.getOrDefault(
                            nodePO.getRuleKey(), Collections.emptyList()))
                    .build());
            if (old != null) {
                throw new IllegalStateException(
                        "规则树存在重复节点，treeId: " + treeId + ", ruleKey: " + nodePO.getRuleKey());
            }
        }
        if (!nodeMap.containsKey(ruleTreePO.getTreeNodeRuleKey())) {
            throw new IllegalStateException(
                    "规则树根节点不存在，treeId: " + treeId
                            + ", root: " + ruleTreePO.getTreeNodeRuleKey());
        }

        // 5. 构建 Rule Tree
        return RuleTreeVO.builder()
                .treeId(ruleTreePO.getTreeId())
                .treeName(ruleTreePO.getTreeName())
                .treeDesc(ruleTreePO.getTreeDesc())
                .treeRootRuleNode(ruleTreePO.getTreeNodeRuleKey())
                .treeNodeMap(nodeMap)
                .build();
    }

    /** 把数据库字符串枚举转换为领域枚举，并在配置错误时给出明确提示。 */
    private RuleTreeNodeLineVO convertLine(RuleTreeNodeLinePO linePO) {
        try {
            return RuleTreeNodeLineVO.builder()
                    .treeId(linePO.getTreeId())
                    .ruleNodeFrom(linePO.getRuleNodeFrom())
                    .ruleNodeTo(linePO.getRuleNodeTo())
                    .ruleLimitType(RuleLimitTypeVO.valueOf(
                            linePO.getRuleLimitType().trim().toUpperCase(Locale.ROOT)))
                    .ruleLimitValue(RuleLogicCheckTypeVO.valueOf(
                            linePO.getRuleLimitValue().trim().toUpperCase(Locale.ROOT)))
                    .build();
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "规则树连线配置非法，treeId: " + linePO.getTreeId()
                            + ", from: " + linePO.getRuleNodeFrom(), ex);
        }
    }

    /**
     * 减少抽奖奖品库存
     * @param cacheKey 缓存key
     * @return 是否成功
     */
    @Override
    public Boolean subtractionAwardStock(String cacheKey) {
        long surplus = redisService.decr(cacheKey);
        if (surplus < 0) {
            redisService.setAtomicLong(cacheKey, 0);
            return false;
        }
        String lockKey = cacheKey + Constants.UNDERLINE + surplus;
        Boolean lock = redisService.setNx(lockKey);
        if (!Boolean.TRUE.equals(lock)) {
            log.info("策略奖品库存防重锁获取失败 lockKey:{}", lockKey);
        }
        return lock;
    }

    /**
     * 缓存抽奖奖品库存
     *
     * @param cacheKey 缓存key
     * @param awardCount 奖品库存
     */
    @Override
    public void cacheStrategyAwardCount(String cacheKey, Integer awardCount) {
        if (awardCount == null || awardCount < 0) {
            throw new IllegalArgumentException("奖品剩余库存不能为 null 或负数");
        }
        redisService.setAtomicLongIfAbsent(cacheKey, awardCount);
    }

    /**
     * 发送奖品库存消耗消息到延迟队列
     *
     * @param strategyAwardStockKeyVO 策略奖品库存键
     */
    @Override
    public void awardStockConsumeSendQueue(StrategyAwardStockKeyVO strategyAwardStockKeyVO) {
        if (strategyAwardStockKeyVO == null
                || strategyAwardStockKeyVO.getStrategyId() == null
                || strategyAwardStockKeyVO.getAwardId() == null) {
            throw new IllegalArgumentException("库存扣减消息参数不能为空");
        }
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_COUNT_QUERY_KEY;

        RBlockingQueue<StrategyAwardStockKeyVO> blockingQueue = redisService.getBlockingQueue(cacheKey);
        RDelayedQueue<StrategyAwardStockKeyVO> delayedQueue = redisService.getDelayedQueue(blockingQueue);
        delayedQueue.offer(strategyAwardStockKeyVO, 3, TimeUnit.SECONDS);
    }

    /**
     * 从延迟队列取出奖品库存消耗消息
     *
     * @return 策略奖品库存键，队列为空时返回 null
     */
    @Override
    public StrategyAwardStockKeyVO takeQueueValue(){
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_COUNT_QUERY_KEY;
        RBlockingQueue<StrategyAwardStockKeyVO> blockingQueue = redisService.getBlockingQueue(cacheKey);
        return blockingQueue.poll();
    }

    /**
     * 更新数据库奖品库存，并失效奖品列表缓存
     *
     * @param strategyId 策略ID
     * @param awardId    奖品ID
     */
    @Override
    public void updateStrategyAwardStock(Long strategyId, Integer awardId) {
        int affectedRows = strategyAwardDao.subtractionAwardStock(strategyId, awardId);
        if (affectedRows != 1) {
            log.warn("数据库奖品库存扣减未生效 strategyId:{} awardId:{}", strategyId, awardId);
            return;
        }
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_LIST_KEY + strategyId;
        redisService.delete(cacheKey);
    }

}
