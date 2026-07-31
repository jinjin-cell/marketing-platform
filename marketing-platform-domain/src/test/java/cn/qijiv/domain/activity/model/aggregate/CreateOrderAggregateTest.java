package cn.qijiv.domain.activity.model.aggregate;

import cn.qijiv.domain.activity.model.entity.ActivityOrderEntity;
import cn.qijiv.domain.activity.model.valobj.OrderStateVO;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 创建订单聚合实体单元测试
 *
 * @author jinlujia
 * @since 2026-07-28
 */
public class CreateOrderAggregateTest {

    @Test
    public void test_builder_createsAggregateWithAllFields() {
        ActivityOrderEntity order = ActivityOrderEntity.builder()
                .userId("user001")
                .activityId(10001L)
                .orderId("order_001")
                .state(OrderStateVO.completed)
                .build();

        CreateOrderAggregate aggregate = CreateOrderAggregate.builder()
                .userId("user001")
                .activityId(10001L)
                .totalCount(10)
                .dayCount(3)
                .monthCount(5)
                .activityOrderEntity(order)
                .build();

        assertNotNull(aggregate);
        assertNotNull(aggregate.getActivityOrderEntity());
        assertEquals("user001", aggregate.getUserId());
        assertEquals(Long.valueOf(10001L), aggregate.getActivityId());
        assertEquals(Integer.valueOf(10), aggregate.getTotalCount());
        assertEquals("order_001", aggregate.getActivityOrderEntity().getOrderId());
    }

    @Test
    public void test_noArgsConstructor_createsEmptyAggregate() {
        CreateOrderAggregate aggregate = new CreateOrderAggregate();
        assertNotNull(aggregate);
        assertNull(aggregate.getUserId());
        assertNull(aggregate.getActivityOrderEntity());
    }

    @Test
    public void test_userId_consistentBetweenAggregateAndOrder() {
        ActivityOrderEntity order = ActivityOrderEntity.builder()
                .userId("user001").build();

        CreateOrderAggregate aggregate = CreateOrderAggregate.builder()
                .userId("user001")
                .activityOrderEntity(order)
                .build();

        assertEquals(aggregate.getUserId(),
                aggregate.getActivityOrderEntity().getUserId());
    }
}
