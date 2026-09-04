package cn.qijiv.test.domain;

import cn.qijiv.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.qijiv.domain.activity.model.entity.ActivityOrderEntity;
import cn.qijiv.domain.activity.model.valobj.OrderStateVO;
import cn.qijiv.domain.activity.repository.IActivityRepository;
import cn.qijiv.domain.activity.service.quota.policy.impl.CreditPayTradePolicy;
import cn.qijiv.domain.credit.model.entity.TradeEntity;
import cn.qijiv.domain.credit.model.valobj.TradeNameVO;
import cn.qijiv.domain.credit.model.valobj.TradeTypeVO;
import cn.qijiv.domain.credit.service.ICreditAdjustService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 积分支付策略测试。 */
public class CreditPayTradePolicyTest {

    /** 支付策略必须先保存待支付订单，再按商品价格扣减用户积分。 */
    @Test
    public void trade_validOrder_savesWaitingOrderAndDeductsCredit() {
        IActivityRepository activityRepository = mock(IActivityRepository.class);
        ICreditAdjustService creditAdjustService = mock(ICreditAdjustService.class);
        CreditPayTradePolicy policy = new CreditPayTradePolicy(activityRepository, creditAdjustService);
        ActivityOrderEntity order = ActivityOrderEntity.builder()
                .userId("user001")
                .orderId("activity-order-001")
                .outBusinessNo("exchange-001")
                .payAmount(new BigDecimal("12.50"))
                .build();
        CreateQuotaOrderAggregate aggregate = CreateQuotaOrderAggregate.builder()
                .userId("user001")
                .activityOrderEntity(order)
                .build();

        policy.trade(aggregate);

        assertEquals(OrderStateVO.wait_pay, order.getState());
        verify(activityRepository).doSaveCreditPayOrder(aggregate);
        ArgumentCaptor<TradeEntity> captor = ArgumentCaptor.forClass(TradeEntity.class);
        verify(creditAdjustService).createOrder(captor.capture());
        TradeEntity trade = captor.getValue();
        assertEquals("user001", trade.getUserId());
        assertEquals(TradeNameVO.CONVERT_SKU, trade.getTradeName());
        assertEquals(TradeTypeVO.REVERSE, trade.getTradeType());
        assertEquals(new BigDecimal("-12.50"), trade.getAmount());
        assertEquals("exchange-001", trade.getOutBusinessNo());
    }

    /** 待支付订单重试时只重试积分扣减，不能重复写活动订单。 */
    @Test
    public void trade_waitingOrder_retriesCreditWithoutSavingDuplicateOrder() {
        IActivityRepository activityRepository = mock(IActivityRepository.class);
        ICreditAdjustService creditAdjustService = mock(ICreditAdjustService.class);
        CreditPayTradePolicy policy = new CreditPayTradePolicy(activityRepository, creditAdjustService);
        ActivityOrderEntity order = ActivityOrderEntity.builder()
                .userId("user001")
                .orderId("activity-order-001")
                .outBusinessNo("exchange-001")
                .payAmount(new BigDecimal("12.50"))
                .state(OrderStateVO.wait_pay)
                .build();
        CreateQuotaOrderAggregate aggregate = CreateQuotaOrderAggregate.builder()
                .userId("user001")
                .activityOrderEntity(order)
                .build();

        policy.trade(aggregate);

        verify(activityRepository, never()).doSaveCreditPayOrder(aggregate);
        verify(creditAdjustService).createOrder(org.mockito.ArgumentMatchers.any(TradeEntity.class));
    }
}
