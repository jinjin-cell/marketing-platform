package cn.qijiv.domain.strategy.model.valobj;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 策略奖品库存Key标识值对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StrategyAwardStockKeyVO {
    private Long strategyId;
    private Integer awardId;
}
