package cn.qijiv.domain.activity.model.entity;

import lombok.Data;

/**
 * 参与抽奖活动实体对象
 *
 * @author qijiv
 * @since 2026/7/18
 */
@Data
public class PartakeRaffleActivityEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 活动ID
     */
    private Long activityId;

}

