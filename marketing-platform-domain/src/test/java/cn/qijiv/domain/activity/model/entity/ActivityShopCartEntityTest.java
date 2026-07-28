package cn.qijiv.domain.activity.model.entity;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 活动购物车实体单元测试
 *
 * @author jinlujia
 * @since 2026-07-28
 */
public class ActivityShopCartEntityTest {

    @Test
    public void test_builder_createsEntityWithAllFields() {
        ActivityShopCartEntity entity = ActivityShopCartEntity.builder()
                .userId("user001")
                .sku(10001L)
                .build();

        assertNotNull(entity);
        assertEquals("user001", entity.getUserId());
        assertEquals(Long.valueOf(10001L), entity.getSku());
    }

    @Test
    public void test_noArgsConstructor_createsEmptyEntity() {
        ActivityShopCartEntity entity = new ActivityShopCartEntity();
        assertNotNull(entity);
        assertNull(entity.getUserId());
        assertNull(entity.getSku());
    }
}
