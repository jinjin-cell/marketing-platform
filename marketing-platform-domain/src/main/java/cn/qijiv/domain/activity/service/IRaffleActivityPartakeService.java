package cn.qijiv.domain.activity.service;

import cn.qijiv.domain.activity.model.entity.PartakeRaffleActivityEntity;
import cn.qijiv.domain.activity.model.entity.UserRaffleOrderEntity;

/**
 * 抽奖活动参与接口
 *
 * @author qijiv
 * @since 2026/7/18
 */
public interface IRaffleActivityPartakeService {


    /**
     * 创建抽奖单；用户参与抽奖活动，扣减活动账户库存，产生抽奖单。如存在未被使用的抽奖单则直接返回已存在的抽奖单。
     *
     * @param userId    用户ID
     * @param activityId 抽奖活动ID
     * @return 用户抽奖订单实体对象
     */
    UserRaffleOrderEntity createOrder(String userId, Long activityId);

    /**
     * 创建抽奖单；用户参与抽奖活动，扣减活动账户库存，产生抽奖单。如存在未被使用的抽奖单则直接返回已存在的抽奖单。
     *
     * @param partakeRaffleActivityEntity 参与抽奖活动实体对象
     * @return 用户抽奖订单实体对象
     */
    UserRaffleOrderEntity createOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity);


}
