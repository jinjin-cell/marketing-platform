package cn.qijiv.api.dto;

import lombok.Data;

/**
 * 抽奖请求参数
 *
 * @author qijiv
 * @since 2026-07-18
 */
@Data
public class RaffleStrategyRequestDTO {

    /** 抽奖策略ID。 */
    private Long strategyId;
}
