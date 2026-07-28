package cn.qijiv.domain.activity.model.aggregate;

import cn.qijiv.domain.activity.model.entity.ActivityAccountEntity;
import cn.qijiv.domain.activity.model.entity.ActivityOrderEntity;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 活动订单聚合实体
 * 
 * @author qijiv
 * @since 2026/7/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderAggregate {

    /**
     * 活动账户实体
     */
    private ActivityAccountEntity activityAccountEntity;
    /**
     * 活动订单实体
     */
    private ActivityOrderEntity activityOrderEntity;

}
