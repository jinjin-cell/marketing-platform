package cn.qijiv.domain.strategy.service.armory;

import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;

import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 策略装配和抽奖调度服务。
 *
 * <p>装配阶段会为同一策略生成两类 Redis 概率表：</p>
 * <ul>
 *     <li>普通概率表：key 为 strategyId，例如 100001</li>
 *     <li>权重概率表：key 为 strategyId_权重配置，例如 100001_4000:102,103,104,105</li>
 * </ul>
 *
 * <p>调度阶段会确保概率表已经装配，再根据表长度生成随机下标，
 * 从 Redis List 中读取单个奖品ID。</p>
 */
@Service
@Slf4j
public class StrategyArmoryDispatch implements IStrategyArmory, IStrategyDispatch {

    private final IStrategyRepository repository;

    public StrategyArmoryDispatch(IStrategyRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean assembleLotteryStrategy(Long strategyId) {
        // 1. 奖品概率是装配基础；没有奖品时不创建任何概率表。
        List<StrategyAwardEntity> strategyAwardList = repository.queryStrategyAwardList(strategyId);
        if (strategyAwardList == null || strategyAwardList.isEmpty()) {
            log.warn("策略奖品列表为空，strategyId: {}", strategyId);
            return false;
        }

        // 2. 策略实体记录了是否启用 rule_weight 等策略级规则。
        StrategyEntity strategyEntity = repository.queryStrategyEntityByStrategyId(strategyId);
        if (strategyEntity == null) {
            log.warn("策略实体为空，strategyId: {}", strategyId);
            return false;
        }

        // 3. 无论是否配置权重规则，都先装配完整奖品范围的基础概率表。
        assembleRateTable(String.valueOf(strategyId), strategyAwardList);

        // 4. 未声明 rule_weight 时，基础概率表装配完成即视为成功。
        String ruleWeight = strategyEntity.getRuleWeight();
        if (ruleWeight == null) {
            return true;
        }

        // 5. 查询 rule_weight 规则，规则值中包含各积分档位允许抽取的奖品ID。
        StrategyRuleEntity strategyRuleEntity = repository.queryStrategyRule(strategyId, ruleWeight);
        if (strategyRuleEntity == null) {
            throw new AppException(ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getCode(),
                    ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getInfo());
        }

        // 6. 每个权重档位过滤出自己的奖品集合，并单独生成一张概率表。
        for (Map.Entry<String, List<Integer>> entry : strategyRuleEntity.getRuleWeightValues().entrySet()) {
            List<StrategyAwardEntity> weightedAwards = strategyAwardList.stream()
                    .filter(entity -> entry.getValue().contains(entity.getAwardId()))
                    .collect(Collectors.toList());
            if (weightedAwards.isEmpty()) {
                throw new IllegalArgumentException("权重规则没有匹配到奖品，ruleWeightValue: " + entry.getKey());
            }
            assembleRateTable(buildStrategyKey(strategyId, entry.getKey()), weightedAwards);
        }

        return true;
    }

    /**
     * 把奖品概率转换为连续的奖品ID槽位，并写入 Redis List。
     *
     * <p>使用所有概率中最大的有效小数位作为统一精度。例如 0.20、0.10、0.04
     * 统一放大100倍后得到20、10、4个槽位，避免用最小概率相除时出现非整数。</p>
     *
     * @param strategyKey       Redis 概率表业务key，可以是策略ID或策略ID+权重规则值
     * @param strategyAwardList 本次允许参与抽奖的奖品配置
     */
    private void assembleRateTable(String strategyKey, List<StrategyAwardEntity> strategyAwardList) {
        // 1. 校验奖品配置，并找出转换为整数槽位所需的最大小数精度。
        int maxScale = 0;
        for (StrategyAwardEntity award : strategyAwardList) {
            if (award == null || award.getAwardId() == null || award.getAwardRate() == null) {
                throw new IllegalArgumentException("策略奖品ID和概率不能为空");
            }
            if (award.getAwardRate().signum() <= 0) {
                throw new IllegalArgumentException("策略奖品概率必须为正数");
            }
            maxScale = Math.max(maxScale, Math.max(0, award.getAwardRate().stripTrailingZeros().scale()));
        }

        // 2. 按统一精度放大概率，并累加得到概率表的总槽位数。
        BigDecimal rateMultiplier = BigDecimal.TEN.pow(maxScale);
        int rateRange = strategyAwardList.stream()
                .mapToInt(award -> award.getAwardRate().multiply(rateMultiplier).intValueExact())
                .sum();

        // 3. 奖品在列表中出现的次数就是它拥有的概率槽位数。
        List<Integer> rateTableList = new ArrayList<>(rateRange);
        for (StrategyAwardEntity award : strategyAwardList) {
            int slots = award.getAwardRate().multiply(rateMultiplier).intValueExact();
            for (int i = 0; i < slots; i++) {
                rateTableList.add(award.getAwardId());
            }
        }

        // 4. 打乱槽位顺序不改变中奖概率，只避免相同奖品连续集中排列。
        Collections.shuffle(rateTableList);
        if (rateTableList.size() != rateRange) {
            throw new IllegalStateException("策略奖品概率无法生成完整概率表");
        }

        // 5. Redis List 下标就是抽奖随机值，因此无需额外构建 Map<下标, 奖品ID>。
        repository.storeStrategyRateTable(strategyKey, rateTableList);
        log.info("策略装配完成 - strategyKey: {}, 查找表大小: {}", strategyKey, rateTableList.size());
    }

    @Override
    public Integer getRandomAwardId(Long strategyId) {
        // 普通抽奖直接使用 strategyId 对应的完整概率表。
        return getRandomAwardIdByKey(strategyId, String.valueOf(strategyId));
    }

    @Override
    public Integer getRandomAwardId(Long strategyId, String ruleWeightValue) {
        if (ruleWeightValue == null || ruleWeightValue.trim().isEmpty()) {
            throw new IllegalArgumentException("权重规则值不能为空");
        }
        // 权重抽奖必须与装配阶段使用完全相同的组合key。
        return getRandomAwardIdByKey(strategyId, buildStrategyKey(strategyId, ruleWeightValue));
    }

    /**
     * 从指定概率表中抽取一个奖品。
     */
    private Integer getRandomAwardIdByKey(Long strategyId, String strategyKey) {
        // Redis LLEN 获取槽位总数，不需要把完整概率表加载回应用。
        Integer rateRange = repository.queryStrategyRateTableSize(strategyKey);
        if (rateRange == null || rateRange <= 0) {
            ensureRateTableInitialized(strategyId, strategyKey);
            rateRange = repository.queryStrategyRateTableSize(strategyKey);
        }
        if (rateRange == null || rateRange <= 0) {
            throw new IllegalStateException("策略概率表装配后仍不可用，strategyKey: " + strategyKey);
        }

        // nextInt 上界不包含 rateRange，生成值正好覆盖 List 的 0..size-1 下标。
        int randomValue = ThreadLocalRandom.current().nextInt(rateRange);
        // Redis LINDEX 按随机下标只返回一个奖品ID。
        Integer awardId = repository.queryStrategyAwardId(strategyKey, randomValue);
        if (awardId == null) {
            throw new IllegalStateException("策略概率表数据不完整，strategyKey: " + strategyKey);
        }

        log.debug("随机抽奖 - strategyKey: {}, randomValue: {}, awardId: {}",
                strategyKey, randomValue, awardId);
        return awardId;
    }

    /**
     * 概率表缺失时按策略重新装配。本地同步用于避免单实例内并发重复装配，
     * Redis 写入端通过临时键原子替换保证多实例同时装配时不会暴露半张表。
     */
    private synchronized void ensureRateTableInitialized(Long strategyId, String strategyKey) {
        Integer currentSize = repository.queryStrategyRateTableSize(strategyKey);
        if (currentSize != null && currentSize > 0) {
            return;
        }
        if (!assembleLotteryStrategy(strategyId)) {
            throw new IllegalStateException("策略概率表初始化失败，strategyId: " + strategyId);
        }
    }

    private String buildStrategyKey(Long strategyId, String ruleWeightValue) {
        // 示例：100001_4000:102,103,104,105。
        return String.valueOf(strategyId).concat("_").concat(ruleWeightValue.trim());
    }
}
