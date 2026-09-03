package cn.qijiv.domain.award.model.entity;

import lombok.*;

/**
 * 分发奖品实体类
 * @author qijiv
 * @since 2026/09/03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributeAwardEntity {

    /**
     * 用户ID
     */
    private String userId;
    /**
     * 订单ID
     */
    private String orderId;
    /**
     * 奖品ID
     */
    private Integer awardId;
    /**
     * 奖品配置信息
     */
    private String awardConfig;

}

