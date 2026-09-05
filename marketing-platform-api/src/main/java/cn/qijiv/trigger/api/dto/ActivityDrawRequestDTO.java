package cn.qijiv.trigger.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 活动抽奖请求参数
 *
 * @author qijiv
 * @since 2026-08-24
 */
@Data
public class ActivityDrawRequestDTO implements Serializable {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 活动ID
     */
    private Long activityId;

}

