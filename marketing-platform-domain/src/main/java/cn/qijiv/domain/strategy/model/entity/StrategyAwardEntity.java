package cn.qijiv.domain.strategy.model.entity;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * 抽奖策略奖品实体
 *
 * @author jinlujia
 * @date 2026/07/18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyAwardEntity {
    /**
     * 抽奖策略ID
     */
    private Long strategyId;

    /**
     * 抽奖奖品ID - 内部流转使用
     */
    private Integer awardId;
    /**
     * 奖品库存总量
     */
    private Integer awardCount;

    /**
     * 奖品库存剩余
     */
    private Integer awardCountSurplus;

    /**
     * 奖品中奖概率
     */
    private BigDecimal awardRate;
}
