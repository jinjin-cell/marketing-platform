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

    /** 验证 Builder 能创建包含全部字段的 SKU 实体。 */
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

    /** 验证无参构造创建的空实体字段均为空。 */
    @Test
    public void test_noArgsConstructor_createsEmptyEntity() {
        ActivitySkuEntity entity = new ActivitySkuEntity();
        assertNotNull(entity);
        assertNull(entity.getSku());
        assertNull(entity.getStockCount());
    }

    /** 验证剩余库存小于总库存。 */
    @Test
    public void test_stockCountSurplus_lessThanStockCount() {
        ActivitySkuEntity entity = ActivitySkuEntity.builder()
                .stockCount(100)
                .stockCountSurplus(30)
                .build();

        assertTrue(entity.getStockCountSurplus() < entity.getStockCount());
    }
}
