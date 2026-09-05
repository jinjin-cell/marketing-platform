package cn.qijiv.trigger.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserActivityAccountRequestDTO implements Serializable {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 活动ID
     */
    private Long activityId;

}
