package cn.qijiv.test.trigger;

import cn.qijiv.domain.activity.model.entity.DeliveryOrderEntity;
import cn.qijiv.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.qijiv.domain.credit.event.CreditAdjustSuccessMessageEvent;
import cn.qijiv.domain.credit.model.valobj.TradeNameVO;
import cn.qijiv.domain.credit.model.valobj.TradeTypeVO;
import cn.qijiv.trigger.listener.CreditAdjustSuccessCustomer;
import cn.qijiv.types.event.BaseEvent;
import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** 积分调整成功消息消费者测试。 */
public class CreditAdjustSuccessCustomerTest {

    /** 普通积分返利不是商品支付，不得触发商品发货。 */
    @Test
    public void listener_rebateCreditMessage_doesNotDeliverProduct() {
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        CreditAdjustSuccessCustomer customer = createCustomer(quotaService);

        customer.listener(messageJson(TradeNameVO.REBATE));

        verifyNoInteractions(quotaService);
    }

    /** 商品兑换扣款成功后应按原业务号完成发货。 */
    @Test
    public void listener_convertSkuMessage_deliversProduct() {
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        CreditAdjustSuccessCustomer customer = createCustomer(quotaService);

        customer.listener(messageJson(TradeNameVO.CONVERT_SKU));

        ArgumentCaptor<DeliveryOrderEntity> captor = ArgumentCaptor.forClass(DeliveryOrderEntity.class);
        verify(quotaService).updateOrder(captor.capture());
        assertEquals("user001", captor.getValue().getUserId());
        assertEquals("exchange-001", captor.getValue().getOutBusinessNo());
    }

    private CreditAdjustSuccessCustomer createCustomer(IRaffleActivityAccountQuotaService quotaService) {
        CreditAdjustSuccessCustomer customer = new CreditAdjustSuccessCustomer();
        ReflectionTestUtils.setField(customer, "topic", "credit_adjust_success");
        ReflectionTestUtils.setField(customer, "raffleActivityAccountQuotaService", quotaService);
        return customer;
    }

    private String messageJson(TradeNameVO tradeName) {
        CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage message =
                CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage.builder()
                        .userId("user001")
                        .orderId("credit-order-001")
                        .amount(new BigDecimal("-12.50"))
                        .tradeName(tradeName.getCode())
                        .tradeType(TradeTypeVO.REVERSE.getCode())
                        .outBusinessNo("exchange-001")
                        .build();
        BaseEvent.EventMessage<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage> event =
                BaseEvent.EventMessage.<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage>builder()
                        .id("message001")
                        .data(message)
                        .build();
        return JSON.toJSONString(event);
    }
}
