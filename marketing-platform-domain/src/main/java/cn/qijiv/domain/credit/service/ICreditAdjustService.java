package cn.qijiv.domain.credit.service;

import cn.qijiv.domain.credit.model.entity.CreditAccountEntity;
import cn.qijiv.domain.credit.model.entity.TradeEntity;

/**
 * 积分调额接口【正逆向，增减积分】
 * @author qijiv
 * @since 2026/9/3
 */
public interface ICreditAdjustService {

    /**
     * 创建积分调额订单并增减账户积分
     * <p>amount 为带符号的增减值：正向入账为正，逆向扣减为负；同一 outBusinessNo 重复调用返回原单号（幂等）。</p>
     *
     * @param tradeEntity 交易实体对象
     * @return 生效的积分订单号
     */
    String createOrder(TradeEntity tradeEntity);

    /**
     * 查询用户积分账户
     *
     * @param userId 用户ID
     * @return 用户积分账户
     */
    CreditAccountEntity queryUserCreditAccount(String userId);
}

