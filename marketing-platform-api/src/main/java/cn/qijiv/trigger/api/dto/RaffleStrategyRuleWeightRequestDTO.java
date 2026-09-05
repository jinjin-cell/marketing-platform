package cn.qijiv.trigger.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 抽奖策略规则权重请求DTO
 * @author qijiv
 * @since  2026/08/31
 */
@Data
public class RaffleStrategyRuleWeightRequestDTO implements Serializable {

    // 用户ID
    private String userId;
    // 抽奖活动ID
    private Long activityId;

}
