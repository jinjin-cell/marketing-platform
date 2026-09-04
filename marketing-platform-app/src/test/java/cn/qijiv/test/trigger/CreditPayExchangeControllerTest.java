package cn.qijiv.test.trigger;

import cn.qijiv.api.dto.CreditPayExchangeRequestDTO;
import cn.qijiv.domain.activity.model.entity.SkuRechargeEntity;
import cn.qijiv.domain.activity.model.valobj.OrderTradeTypeVO;
import cn.qijiv.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.qijiv.trigger.http.RaffleActivityController;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.model.Response;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 积分兑换商品接口测试。 */
public class CreditPayExchangeControllerTest {

    /** 合法请求应创建积分支付订单并返回活动订单号。 */
    @Test
    public void creditPayExchangeSku_validRequest_createsCreditPayOrder() {
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        RaffleActivityController controller = new RaffleActivityController();
        ReflectionTestUtils.setField(controller, "raffleActivityAccountQuotaService", quotaService);
        CreditPayExchangeRequestDTO request = new CreditPayExchangeRequestDTO();
        request.setUserId("user001");
        request.setSku(9011L);
        request.setOutBusinessNo("exchange-001");
        when(quotaService.createSkuRechargeOrder(org.mockito.ArgumentMatchers.any(SkuRechargeEntity.class)))
                .thenReturn("activity-order-001");

        Response<String> response = controller.creditPayExchangeSku(request);

        assertEquals(ResponseCode.SUCCESS.getCode(), response.getCode());
        assertEquals("activity-order-001", response.getData());
        ArgumentCaptor<SkuRechargeEntity> captor = ArgumentCaptor.forClass(SkuRechargeEntity.class);
        verify(quotaService).createSkuRechargeOrder(captor.capture());
        assertEquals("user001", captor.getValue().getUserId());
        assertEquals(Long.valueOf(9011L), captor.getValue().getSku());
        assertEquals("exchange-001", captor.getValue().getOutBusinessNo());
        assertEquals(OrderTradeTypeVO.credit_pay_trade, captor.getValue().getOrderTradeType());
    }

    /** 缺少业务幂等号时不得创建支付订单。 */
    @Test
    public void creditPayExchangeSku_missingBusinessNo_rejectsRequest() {
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        RaffleActivityController controller = new RaffleActivityController();
        ReflectionTestUtils.setField(controller, "raffleActivityAccountQuotaService", quotaService);
        CreditPayExchangeRequestDTO request = new CreditPayExchangeRequestDTO();
        request.setUserId("user001");
        request.setSku(9011L);

        Response<String> response = controller.creditPayExchangeSku(request);

        assertEquals(ResponseCode.ILLEGAL_PARAMETER.getCode(), response.getCode());
        verify(quotaService, never()).createSkuRechargeOrder(org.mockito.ArgumentMatchers.any(SkuRechargeEntity.class));
    }
}
