package cn.qijiv.domain.activity.model.aggregate;

import cn.qijiv.domain.activity.model.entity.ActivityAccountEntity;
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
        ActivityAccountEntity account = ActivityAccountEntity.builder()
                .userId("user001")
                .activityId(10001L)
                .totalCount(10)
                .totalCountSurplus(5)
                .build();

        ActivityOrderEntity order = ActivityOrderEntity.builder()
                .userId("user001")
                .activityId(10001L)
                .orderId("order_001")
                .state(OrderStateVO.completed)
                .build();

        CreateOrderAggregate aggregate = CreateOrderAggregate.builder()
                .activityAccountEntity(account)
                .activityOrderEntity(order)
                .build();

        assertNotNull(aggregate);
        assertNotNull(aggregate.getActivityAccountEntity());
        assertNotNull(aggregate.getActivityOrderEntity());
        assertEquals("user001", aggregate.getActivityAccountEntity().getUserId());
        assertEquals("order_001", aggregate.getActivityOrderEntity().getOrderId());
    }

    @Test
    public void test_noArgsConstructor_createsEmptyAggregate() {
        CreateOrderAggregate aggregate = new CreateOrderAggregate();
        assertNotNull(aggregate);
        assertNull(aggregate.getActivityAccountEntity());
        assertNull(aggregate.getActivityOrderEntity());
    }

    @Test
    public void test_userId_consistentBetweenAccountAndOrder() {
        ActivityAccountEntity account = ActivityAccountEntity.builder()
                .userId("user001").build();
        ActivityOrderEntity order = ActivityOrderEntity.builder()
                .userId("user001").build();

        CreateOrderAggregate aggregate = CreateOrderAggregate.builder()
                .activityAccountEntity(account)
                .activityOrderEntity(order)
                .build();

        assertEquals(aggregate.getActivityAccountEntity().getUserId(),
                aggregate.getActivityOrderEntity().getUserId());
    }
}
