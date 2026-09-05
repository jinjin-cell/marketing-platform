package cn.qijiv.trigger.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 抽奖奖品列表查询请求参数
 *
 * @author qijiv
 * @since 2026-07-18
 */
@Data
public class RaffleAwardListRequestDTO implements Serializable {

    /** 抽奖策略ID。 */
    @Deprecated
    private Long strategyId;
    /** 活动ID。 */
    private Long activityId;
    /** 用户ID。 */
    private String userId;
}
