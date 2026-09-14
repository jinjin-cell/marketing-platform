package cn.qijiv.domain.activity.service;

import cn.qijiv.domain.activity.model.entity.ActivityEntity;
import cn.qijiv.domain.activity.model.entity.ActivityOrderEntity;

import java.util.List;

/**
 * 活动查询服务【活动配置与活动订单的只读查询】
 *
 * @author qijiv
 * @since 2026/09/14
 */
public interface IRaffleActivityQueryService {

    /**
     * 查询全部活动配置，用于前端活动列表与多活动切换
     *
     * @return 活动列表（活动ID升序）
     */
    List<ActivityEntity> queryActivityList();

    /**
     * 查询用户的活动订单（兑换/充值记录）
     *
     * @param userId 用户ID
     * @return 活动订单列表（下单时间倒序）
     */
    List<ActivityOrderEntity> queryActivityOrderList(String userId);
}
