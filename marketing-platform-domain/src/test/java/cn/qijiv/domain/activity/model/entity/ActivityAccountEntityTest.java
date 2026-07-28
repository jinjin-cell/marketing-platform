package cn.qijiv.domain.activity.model.entity;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 活动账户实体单元测试
 *
 * @author jinlujia
 * @since 2026-07-28
 */
public class ActivityAccountEntityTest {

    @Test
    public void test_builder_createsEntityWithAllFields() {
        ActivityAccountEntity entity = ActivityAccountEntity.builder()
                .userId("user001")
                .activityId(10001L)
                .totalCount(10)
                .totalCountSurplus(5)
                .dayCount(3)
                .dayCountSurplus(2)
                .monthCount(5)
                .monthCountSurplus(3)
                .build();

        assertNotNull(entity);
        assertEquals("user001", entity.getUserId());
        assertEquals(Long.valueOf(10001L), entity.getActivityId());
        assertEquals(Integer.valueOf(10), entity.getTotalCount());
        assertEquals(Integer.valueOf(5), entity.getTotalCountSurplus());
        assertEquals(Integer.valueOf(3), entity.getDayCount());
        assertEquals(Integer.valueOf(2), entity.getDayCountSurplus());
        assertEquals(Integer.valueOf(5), entity.getMonthCount());
        assertEquals(Integer.valueOf(3), entity.getMonthCountSurplus());
    }

    @Test
    public void test_surplusCounts_notExceedTotalCounts() {
        ActivityAccountEntity entity = ActivityAccountEntity.builder()
                .totalCount(10)
                .totalCountSurplus(5)
                .dayCount(3)
                .dayCountSurplus(2)
                .monthCount(5)
                .monthCountSurplus(3)
                .build();

        assertTrue(entity.getTotalCountSurplus() <= entity.getTotalCount());
        assertTrue(entity.getDayCountSurplus() <= entity.getDayCount());
        assertTrue(entity.getMonthCountSurplus() <= entity.getMonthCount());
    }

    @Test
    public void test_noArgsConstructor_createsEmptyEntity() {
        ActivityAccountEntity entity = new ActivityAccountEntity();
        assertNotNull(entity);
        assertNull(entity.getUserId());
    }
}
