package cn.qijiv.api.dto;

import lombok.Data;

/**
 * 抽奖奖品列表查询请求参数
 *
 * @author qijiv
 * @since 2026-07-18
 */
@Data
public class RaffleAwardListRequestDTO {

    /** 抽奖策略ID。 */
    private Long strategyId;
}
