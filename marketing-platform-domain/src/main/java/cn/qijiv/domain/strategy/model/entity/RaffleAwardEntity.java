package cn.qijiv.domain.strategy.model.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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
public class RaffleAwardEntity {

    /**
     * 抽奖策略ID
     */
    private Long strategyId;
    /**
     * 抽奖奖品ID - 内部流转使用
     */
    private Integer awardId;
    /**
     * 奖品key - 外部展示使用
     */
    private String awardKey;
    /**
     * 奖品配置 - 内部流转使用
     */
    private String awardConfig;
    /**
     * 奖品描述 - 外部展示使用
     */
    private String awardDesc;
}
