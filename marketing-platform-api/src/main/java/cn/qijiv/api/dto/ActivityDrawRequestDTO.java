package cn.qijiv.api.dto;

import lombok.Data;

/**
 * 活动抽奖请求参数
 *
 * @author qijiv
 * @since 2026-08-24
 */
@Data
public class ActivityDrawRequestDTO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 活动ID
     */
    private Long activityId;

}

