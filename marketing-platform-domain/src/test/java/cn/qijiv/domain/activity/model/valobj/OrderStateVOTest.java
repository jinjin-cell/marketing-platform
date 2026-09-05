package cn.qijiv.domain.activity.model.valobj;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 订单状态枚举单元测试
 *
 * @author jinlujia
 * @since 2026-07-28
 */
public class OrderStateVOTest {

    /** 验证 completed（完成）状态的编码与描述。 */
    @Test
    public void test_completed_state() {
        assertEquals("completed", OrderStateVO.completed.getCode());
        assertEquals("完成", OrderStateVO.completed.getDesc());
    }

    /** 验证 wait_pay（待支付）状态的编码与描述。 */
    @Test
    public void test_waitPay_state() {
        assertEquals("wait_pay", OrderStateVO.wait_pay.getCode());
        assertEquals("待支付", OrderStateVO.wait_pay.getDesc());
    }

    /** 验证 expired（已过期）状态的编码与描述。 */
    @Test
    public void test_expired_state() {
        assertEquals("expired", OrderStateVO.expired.getCode());
        assertEquals("已过期", OrderStateVO.expired.getDesc());
    }

    /** 验证 valueOf 能根据编码解析出正确的枚举。 */
    @Test
    public void test_valueOf_returnsCorrectEnum() {
        assertEquals(OrderStateVO.completed, OrderStateVO.valueOf("completed"));
    }

    /** 验证传入非法编码时 valueOf 抛出 IllegalArgumentException。 */
    @Test(expected = IllegalArgumentException.class)
    public void test_valueOf_invalidValue_throwsException() {
        OrderStateVO.valueOf("invalid");
    }
}
