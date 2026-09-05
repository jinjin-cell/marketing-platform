package cn.qijiv.domain.credit.repository;

import cn.qijiv.domain.credit.model.aggregate.TradeAggregate;
import cn.qijiv.domain.credit.model.entity.CreditAccountEntity;

/**
 * 用户积分仓储
 * @author qijiv
 * @since 2026/9/3
 */
public interface ICreditRepository {

    /**
     * 保存用户积分交易，按外部业务号保证幂等。
     *
     * @param tradeAggregate 积分交易聚合
     * @return 生效的积分订单号；重复请求返回原订单号
     */
    String saveUserCreditTradeOrder(TradeAggregate tradeAggregate);

    /**
     * 查询用户积分账户
     *
     * @param userId 用户ID
     * @return 用户积分账户
     */
    CreditAccountEntity queryUserCreditAccount(String userId);
}

