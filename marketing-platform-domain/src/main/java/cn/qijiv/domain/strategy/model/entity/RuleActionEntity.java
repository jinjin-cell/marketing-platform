package cn.qijiv.domain.strategy.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抽奖规则执行结果。
 *
 * @param <T> 规则阶段对应的数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleActionEntity<T extends RuleActionEntity.RaffleEntity> {

    /** 命中的规则模型 */
    private String ruleModel;
    /** 规则检查结果编码 */
    private String code;
    /** 规则检查结果说明 */
    private String info;
    /** 规则接管抽奖时返回的数据 */
    private T data;

    /** 规则阶段数据的统一标记接口。 */
    public interface RaffleEntity {
    }

    /** 抽奖前置规则返回的数据。 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RaffleBeforeEntity implements RaffleEntity {

        /** 抽奖策略ID */
        private Long strategyId;
        /** 黑名单规则指定的奖品ID */
        private Integer awardId;
        /** 权重规则命中的完整档位配置 */
        private String ruleWeightValueKey;
    }
}
