package cn.qijiv.test.domain;

import cn.qijiv.domain.credit.event.CreditAdjustSuccessMessageEvent;
import cn.qijiv.domain.credit.model.entity.TradeEntity;
import cn.qijiv.domain.credit.model.aggregate.TradeAggregate;
import cn.qijiv.domain.credit.model.valobj.TradeNameVO;
import cn.qijiv.domain.credit.model.valobj.TradeTypeVO;
import cn.qijiv.domain.credit.repository.ICreditRepository;
import cn.qijiv.domain.credit.service.CreditAdjustService;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/** 积分调额服务的领域输入约束测试。 */
public class CreditAdjustServiceTest {

    /** 合法交易应生成完整聚合并返回仓储确认的订单号。 */
    @Test
    public void createOrder_validTrade_persistsCompleteTradeAggregate() throws Exception {
        CreditAdjustService service = new CreditAdjustService();
        CapturingRepository repository = new CapturingRepository();
        setField(service, "creditRepository", repository);
        CreditAdjustSuccessMessageEvent event = new CreditAdjustSuccessMessageEvent();
        setField(event, "topic", "credit_adjust_success");
        setField(service, "creditAdjustSuccessMessageEvent", event);

        String orderId = service.createOrder(TradeEntity.builder()
                .userId("user001")
                .tradeName(TradeNameVO.REBATE)
                .tradeType(TradeTypeVO.REVERSE)
                .amount(new BigDecimal("-3.50"))
                .outBusinessNo("rebate-002")
                .build());

        assertEquals("order-001", orderId);
        assertNotNull(repository.aggregate);
        assertEquals("user001", repository.aggregate.getUserId());
        assertEquals(new BigDecimal("-3.50"), repository.aggregate.getCreditAccountEntity().getAdjustAmount());
        assertEquals(TradeNameVO.REBATE, repository.aggregate.getCreditOrderEntity().getTradeName());
        assertEquals(TradeTypeVO.REVERSE, repository.aggregate.getCreditOrderEntity().getTradeType());
        assertEquals("rebate-002", repository.aggregate.getCreditOrderEntity().getOutBusinessNo());
        assertNotNull(repository.aggregate.getTaskEntity());
        assertEquals("user001", repository.aggregate.getTaskEntity().getUserId());
        assertEquals("credit_adjust_success", repository.aggregate.getTaskEntity().getTopic());
        assertEquals("order-001", orderId);
        CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage message =
                repository.aggregate.getTaskEntity().getMessage().getData();
        assertEquals("user001", message.getUserId());
        assertEquals("rebate-002", message.getOutBusinessNo());
        assertEquals(TradeNameVO.REBATE.getCode(), message.getTradeName());
        assertEquals(TradeTypeVO.REVERSE.getCode(), message.getTradeType());
    }

    /** 零金额若允许落单，会产生无法反映任何资金变动的积分流水。 */
    @Test
    public void createOrder_zeroAmount_rejectsInvalidTradeBeforePersistence() throws Exception {
        CreditAdjustService service = new CreditAdjustService();
        setField(service, "creditRepository", new FailingRepository());

        try {
            service.createOrder(TradeEntity.builder()
                    .userId("user001")
                    .tradeName(TradeNameVO.REBATE)
                    .tradeType(TradeTypeVO.FORWARD)
                    .amount(BigDecimal.ZERO)
                    .outBusinessNo("rebate-001")
                    .build());
            fail("零金额交易应在持久化前被拒绝");
        } catch (AppException e) {
            assertEquals(ResponseCode.ILLEGAL_PARAMETER.getCode(), e.getCode());
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FailingRepository implements ICreditRepository {

        @Override
        public String saveUserCreditTradeOrder(cn.qijiv.domain.credit.model.aggregate.TradeAggregate tradeAggregate) {
            fail("非法交易不应进入持久化层");
            return null;
        }

        @Override
        public cn.qijiv.domain.credit.model.entity.CreditAccountEntity queryUserCreditAccount(String userId) {
            fail("非法交易不应触发账户查询");
            return null;
        }
    }

    private static class CapturingRepository implements ICreditRepository {

        private TradeAggregate aggregate;

        @Override
        public String saveUserCreditTradeOrder(TradeAggregate tradeAggregate) {
            aggregate = tradeAggregate;
            return "order-001";
        }

        @Override
        public cn.qijiv.domain.credit.model.entity.CreditAccountEntity queryUserCreditAccount(String userId) {
            return null;
        }
    }
}
