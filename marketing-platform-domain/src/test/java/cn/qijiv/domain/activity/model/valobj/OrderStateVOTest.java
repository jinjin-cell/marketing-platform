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

    @Test
    public void test_completed_state() {
        assertEquals("completed", OrderStateVO.completed.getCode());
        assertEquals("完成", OrderStateVO.completed.getDesc());
    }

    @Test
    public void test_valueOf_returnsCorrectEnum() {
        assertEquals(OrderStateVO.completed, OrderStateVO.valueOf("completed"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_valueOf_invalidValue_throwsException() {
        OrderStateVO.valueOf("invalid");
    }
}
