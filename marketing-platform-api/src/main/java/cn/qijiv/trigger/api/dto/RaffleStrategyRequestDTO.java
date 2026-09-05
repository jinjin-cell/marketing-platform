package cn.qijiv.trigger.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 抽奖请求参数
 *
 * @author qijiv
 * @since 2026-07-18
 */
@Data
public class RaffleStrategyRequestDTO implements Serializable {

    /** 抽奖策略ID。 */
    private Long strategyId;
}
