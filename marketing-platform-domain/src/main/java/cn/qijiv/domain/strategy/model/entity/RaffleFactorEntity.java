package cn.qijiv.domain.strategy.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 抽奖因子实体类
 *
 * @author jinlujia
 * @since 2026-07-18
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
    /** 活动结束时间，用于设置奖品库存锁缓存的有效期 */
    private Date endDateTime;
}
