package cn.qijiv.domain.activity.service.quota.policy;

import cn.qijiv.domain.activity.model.aggregate.CreateQuotaOrderAggregate;

/**
 * 交易策略接口，包括；返利兑换（不用支付），积分订单（需要支付）
 * @author qijiv
 * @since  2026/9/4
 */
public interface ITradePolicy {

    void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate);

}
