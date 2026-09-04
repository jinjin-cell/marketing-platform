package cn.qijiv.domain.activity.service.quota.policy.impl;

import cn.qijiv.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.qijiv.domain.activity.model.valobj.OrderStateVO;
import cn.qijiv.domain.activity.repository.IActivityRepository;
import cn.qijiv.domain.activity.service.quota.policy.ITradePolicy;
import cn.qijiv.domain.credit.model.entity.TradeEntity;
import cn.qijiv.domain.credit.model.valobj.TradeNameVO;
import cn.qijiv.domain.credit.model.valobj.TradeTypeVO;
import cn.qijiv.domain.credit.service.ICreditAdjustService;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 积分兑换，支付类订单
 *
 */
@Service("credit_pay_trade")
public class CreditPayTradePolicy implements ITradePolicy {

    private final IActivityRepository activityRepository;
    private final ICreditAdjustService creditAdjustService;

    public CreditPayTradePolicy(IActivityRepository activityRepository, ICreditAdjustService creditAdjustService) {
        this.activityRepository = activityRepository;
        this.creditAdjustService = creditAdjustService;
    }

    @Override
    public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        BigDecimal payAmount = createQuotaOrderAggregate.getActivityOrderEntity().getPayAmount();
        if (null == payAmount || payAmount.signum() <= 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "商品积分价格必须大于0");
        }

        if (OrderStateVO.wait_pay != createQuotaOrderAggregate.getActivityOrderEntity().getState()) {
            createQuotaOrderAggregate.setOrderState(OrderStateVO.wait_pay);
            activityRepository.doSaveCreditPayOrder(createQuotaOrderAggregate);
        }

        creditAdjustService.createOrder(TradeEntity.builder()
                .userId(createQuotaOrderAggregate.getUserId())
                .tradeName(TradeNameVO.CONVERT_SKU)
                .tradeType(TradeTypeVO.REVERSE)
                .amount(payAmount.negate())
                .outBusinessNo(createQuotaOrderAggregate.getActivityOrderEntity().getOutBusinessNo())
                .build());
    }

}
