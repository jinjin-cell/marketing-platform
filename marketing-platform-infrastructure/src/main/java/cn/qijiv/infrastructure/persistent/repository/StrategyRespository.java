package cn.qijiv.infrastructure.persistent.repository;

import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.infrastructure.persistent.dao.IStrategyAwardDao;
import cn.qijiv.infrastructure.persistent.po.StrategyAwardPO;
import cn.qijiv.domain.strategy.model.StrategyAwardEntity;

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
    public void storeStrategyRateTable(Long strategyId, List<Integer> rateTable) {
        String cacheKey = Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + strategyId;
        redisService.setList(cacheKey, rateTable);
    }

    /**
     * 查询策略概率表的槽位数量，作为随机数上界
     *
     * @param strategyId 策略ID
     * @return 概率表槽位数量
     */
    @Override
    public Integer queryStrategyRateTableSize(Long strategyId) {
        String cacheKey = Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + strategyId;
        int size = redisService.getListSize(cacheKey);
        return size > 0 ? size : null;
    }

    /**
     * 按随机值查询策略奖品 ID
     *
     * @param strategyId  策略ID
     * @param randomValue 概率表下标
     * @return 奖品ID
     */
    @Override
    public Integer queryStrategyAwardId(Long strategyId, Integer randomValue) {
        String cacheKey = Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + strategyId;
        return redisService.getListValue(cacheKey, randomValue);
    }

}
