package cn.qijiv.domain.strategy.model.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 抽奖策略奖品实体
 *
 * @author jinlujia
 * @since 2026-07-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaffleAwardEntity {

    /** 奖品ID */
    private Integer awardId;
    /** 奖品配置信息 */
    private String awardConfig;
    /** 奖品顺序号 */
    private Integer sort;
}
