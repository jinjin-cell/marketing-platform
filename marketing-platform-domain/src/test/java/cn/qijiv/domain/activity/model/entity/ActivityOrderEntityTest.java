package cn.qijiv.domain.activity.model.entity;

import cn.qijiv.domain.activity.model.valobj.OrderStateVO;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * 活动订单实体单元测试
 *
 * @author jinlujia
 * @since 2026-07-28
 */
public class ActivityOrderEntityTest {

    @Test
    public void test_builder_createsEntityWithAllFields() {
        Date orderTime = new Date();
        ActivityOrderEntity entity = ActivityOrderEntity.builder()
                .userId("user001")
                .activityId(10001L)
                .activityName("测试活动")
                .strategyId(20001L)
                .orderId("order_001")
                .orderTime(orderTime)
                .totalCount(10)
                .dayCount(3)
                .monthCount(5)
                .state(OrderStateVO.completed)
                .build();

        assertNotNull(entity);
        assertEquals("user001", entity.getUserId());
        assertEquals(Long.valueOf(10001L), entity.getActivityId());
        assertEquals("测试活动", entity.getActivityName());
        assertEquals(Long.valueOf(20001L), entity.getStrategyId());
        assertEquals("order_001", entity.getOrderId());
        assertEquals(orderTime, entity.getOrderTime());
        assertEquals(Integer.valueOf(10), entity.getTotalCount());
        assertEquals(Integer.valueOf(3), entity.getDayCount());
        assertEquals(Integer.valueOf(5), entity.getMonthCount());
        assertEquals(OrderStateVO.completed, entity.getState());
    }

    @Test
    public void test_noArgsConstructor_createsEmptyEntity() {
        ActivityOrderEntity entity = new ActivityOrderEntity();
        assertNotNull(entity);
        assertNull(entity.getUserId());
        assertNull(entity.getState());
    }
}
