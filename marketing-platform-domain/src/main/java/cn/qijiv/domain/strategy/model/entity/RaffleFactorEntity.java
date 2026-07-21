package cn.qijiv.domain.strategy.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抽奖因子实体类
 *
 * @author jinlujia
 * @date 2026/07/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RaffleFactorEntity {

    /** 用户ID */
   private String userId;
    /** 抽奖策略ID */
   private Long strategyId;
}
