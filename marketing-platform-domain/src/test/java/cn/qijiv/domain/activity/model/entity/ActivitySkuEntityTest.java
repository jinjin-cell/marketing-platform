package cn.qijiv.domain.activity.model.entity;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 活动SKU实体单元测试
 *
 * @author jinlujia
 * @since 2026-07-28
 */
public class ActivitySkuEntityTest {

    @Test
    public void test_builder_createsEntityWithAllFields() {
        ActivitySkuEntity entity = ActivitySkuEntity.builder()
                .sku(10001L)
                .activityId(20001L)
                .activityCountId(30001L)
                .stockCount(100)
                .stockCountSurplus(50)
                .build();

        assertNotNull(entity);
        assertEquals(Long.valueOf(10001L), entity.getSku());
        assertEquals(Long.valueOf(20001L), entity.getActivityId());
        assertEquals(Long.valueOf(30001L), entity.getActivityCountId());
        assertEquals(Integer.valueOf(100), entity.getStockCount());
        assertEquals(Integer.valueOf(50), entity.getStockCountSurplus());
    }

    @Test
    public void test_noArgsConstructor_createsEmptyEntity() {
        ActivitySkuEntity entity = new ActivitySkuEntity();
        assertNotNull(entity);
        assertNull(entity.getSku());
        assertNull(entity.getStockCount());
    }

    @Test
    public void test_stockCountSurplus_lessThanStockCount() {
        ActivitySkuEntity entity = ActivitySkuEntity.builder()
                .stockCount(100)
                .stockCountSurplus(30)
                .build();

        assertTrue(entity.getStockCountSurplus() < entity.getStockCount());
    }
}
