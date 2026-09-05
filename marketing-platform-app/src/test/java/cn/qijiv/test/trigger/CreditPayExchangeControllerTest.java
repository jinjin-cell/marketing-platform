package cn.qijiv.test.trigger;

import cn.qijiv.api.dto.SkuProductShopCartRequestDTO;
import cn.qijiv.domain.activity.model.entity.SkuRechargeEntity;
import cn.qijiv.domain.activity.model.entity.UnpaidActivityOrderEntity;
import cn.qijiv.domain.activity.model.valobj.OrderTradeTypeVO;
import cn.qijiv.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.qijiv.domain.credit.model.entity.TradeEntity;
import cn.qijiv.domain.credit.model.valobj.TradeNameVO;
import cn.qijiv.domain.credit.model.valobj.TradeTypeVO;
import cn.qijiv.domain.credit.service.ICreditAdjustService;
import cn.qijiv.trigger.http.RaffleActivityController;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.model.Response;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 积分兑换商品接口测试。 */
public class CreditPayExchangeControllerTest {

    /** 合法请求应创建积分支付订单并完成支付。 */
    @Test
    public void creditPayExchangeSku_validRequest_createsCreditPayOrder() {
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        ICreditAdjustService creditAdjustService = mock(ICreditAdjustService.class);
        RaffleActivityController controller = new RaffleActivityController();
        ReflectionTestUtils.setField(controller, "raffleActivityAccountQuotaService", quotaService);
        ReflectionTestUtils.setField(controller, "creditAdjustService", creditAdjustService);

        UnpaidActivityOrderEntity unpaidOrder = UnpaidActivityOrderEntity.builder()
                .userId("user001")
                .orderId("activity-order-001")
                .outBusinessNo("exchange-001")
                .payAmount(new BigDecimal("12.50"))
                .build();
        when(quotaService.createSkuRechargeOrder(any(SkuRechargeEntity.class))).thenReturn(unpaidOrder);
        when(creditAdjustService.createOrder(any(TradeEntity.class))).thenReturn("credit-order-001");

        SkuProductShopCartRequestDTO request = new SkuProductShopCartRequestDTO();
        request.setUserId("user001");
        request.setSku(9011L);

        Response<Boolean> response = controller.creditPayExchangeSku(request);

        assertEquals(ResponseCode.SUCCESS.getCode(), response.getCode());
        assertEquals(Boolean.TRUE, response.getData());

        ArgumentCaptor<SkuRechargeEntity> captor = ArgumentCaptor.forClass(SkuRechargeEntity.class);
        verify(quotaService).createSkuRechargeOrder(captor.capture());
        assertEquals("user001", captor.getValue().getUserId());
        assertEquals(Long.valueOf(9011L), captor.getValue().getSku());
        assertEquals(OrderTradeTypeVO.credit_pay_trade, captor.getValue().getOrderTradeType());

        ArgumentCaptor<TradeEntity> tradeCaptor = ArgumentCaptor.forClass(TradeEntity.class);
        verify(creditAdjustService).createOrder(tradeCaptor.capture());
        assertEquals("user001", tradeCaptor.getValue().getUserId());
        assertEquals(TradeNameVO.CONVERT_SKU, tradeCaptor.getValue().getTradeName());
        assertEquals(TradeTypeVO.REVERSE, tradeCaptor.getValue().getTradeType());
        assertEquals(new BigDecimal("-12.50"), tradeCaptor.getValue().getAmount());
        assertEquals("exchange-001", tradeCaptor.getValue().getOutBusinessNo());
    }

    /** 缺少用户或 SKU 时不得创建支付订单。 */
    @Test
    public void creditPayExchangeSku_missingSku_rejectsRequest() {
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        ICreditAdjustService creditAdjustService = mock(ICreditAdjustService.class);
        RaffleActivityController controller = new RaffleActivityController();
        ReflectionTestUtils.setField(controller, "raffleActivityAccountQuotaService", quotaService);
        ReflectionTestUtils.setField(controller, "creditAdjustService", creditAdjustService);

        SkuProductShopCartRequestDTO request = new SkuProductShopCartRequestDTO();
        request.setUserId("user001");

        Response<Boolean> response = controller.creditPayExchangeSku(request);

        assertEquals(ResponseCode.ILLEGAL_PARAMETER.getCode(), response.getCode());
        verify(quotaService, never()).createSkuRechargeOrder(any(SkuRechargeEntity.class));
        verify(creditAdjustService, never()).createOrder(any(TradeEntity.class));
    }
}
