package cn.qijiv.domain.credit.repository;

import cn.qijiv.domain.credit.model.aggregate.TradeAggregate;
import cn.qijiv.domain.credit.model.entity.CreditAccountEntity;
import cn.qijiv.domain.credit.model.entity.CreditOrderRecordEntity;

import java.util.List;

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

    /**
     * 查询用户积分流水，按交易时间倒序
     *
     * @param userId 用户ID
     * @param limit  最大返回条数
     * @return 积分流水列表
     */
    List<CreditOrderRecordEntity> queryCreditOrderRecordList(String userId, Integer limit);
}

