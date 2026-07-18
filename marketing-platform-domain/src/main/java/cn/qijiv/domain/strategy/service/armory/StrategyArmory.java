package cn.qijiv.domain.strategy.service.armory;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.model.StrategyAwardEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 策略兵工厂 —— 负责策略数据装配
 *
 * @author jinlujia
 * @date 2026/07/18
 */
@Service
@Slf4j
public class StrategyArmory implements IStrategyArmory {

    private final IStrategyRepository repository;

    public StrategyArmory(IStrategyRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean assembleLotteryStrategy(Long strategyId) {
        // 1. 查询策略奖品配置
        List<StrategyAwardEntity> strategyAwardList = repository.queryStrategyAwardList(strategyId);
        if (strategyAwardList == null || strategyAwardList.isEmpty()) {
            log.warn("策略奖品列表为空，strategyId: {}", strategyId);
            return false;
        }

        // 2. 获取最小概率值
        BigDecimal minRate = strategyAwardList.stream()
                .map(StrategyAwardEntity::getAwardRate)
                .min(BigDecimal::compareTo)
                .orElseThrow(() -> new IllegalArgumentException("概率值不能为空"));

        // 3. 获取概率值总和
        BigDecimal totalRate = strategyAwardList.stream()
                .map(StrategyAwardEntity::getAwardRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (minRate.signum() <= 0 || totalRate.signum() <= 0) {
            throw new IllegalArgumentException("策略奖品概率必须为正数");
        }

        // 4. 用总概率 / 最小概率生成随机范围，保证范围与概率表槽位数一致。
        int rateRange = totalRate.divide(minRate, 0, RoundingMode.UNNECESSARY).intValueExact();

        log.info("策略装配 - strategyId: {}, minRate: {}, totalRate: {}, rateRange: {}",
                strategyId, minRate, totalRate, rateRange);

        // 5. 生成策略奖品概率查找表（按概率占比填充奖品ID占位）
        List<Integer> rateTableList = new ArrayList<>(rateRange);
        for (StrategyAwardEntity award : strategyAwardList) {
            int slots = award.getAwardRate().divide(minRate, 0, RoundingMode.UNNECESSARY).intValueExact();
            for (int i = 0; i < slots; i++) {
                rateTableList.add(award.getAwardId());
            }
        }

        // 6. 乱序操作 —— 避免随机数顺序命中固定奖品
        Collections.shuffle(rateTableList);

        // 7. 校验概率表完整性，列表下标直接作为随机值
        if (rateTableList.size() != rateRange) {
            throw new IllegalStateException("策略奖品概率无法生成完整概率表");
        }

        // 8. 存放到 Redis
        repository.storeStrategyRateTable(strategyId, rateTableList);

        log.info("策略装配完成 - strategyId: {}, 查找表大小: {}", strategyId, rateTableList.size());
        return true;
    }

    @Override
    public Integer getRandomAwardId(Long strategyId) {
        // 1. 从 Redis 获取概率表长度，作为随机数范围
        Integer rateRange = repository.queryStrategyRateTableSize(strategyId);
        if (rateRange == null || rateRange <= 0) {
            throw new IllegalStateException("策略概率范围未初始化，请先调用 assembleLotteryStrategy，strategyId: " + strategyId);
        }

        // 2. 生成随机值
        int randomValue = ThreadLocalRandom.current().nextInt(rateRange);

        // 3. 从 Redis 按随机值获取命中奖品 ID，避免加载整张概率表
        Integer awardId = repository.queryStrategyAwardId(strategyId, randomValue);
        if (awardId == null) {
            throw new IllegalStateException("策略概率查找表未初始化，请先调用 assembleLotteryStrategy，strategyId: " + strategyId);
        }

        log.debug("随机抽奖 - strategyId: {}, randomValue: {}, awardId: {}", strategyId, randomValue, awardId);
        return awardId;
    }
}
