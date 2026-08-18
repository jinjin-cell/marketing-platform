package cn.qijiv.domain.activity.model.entity;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 活动次数实体单元测试
 *
 * @author jinlujia
 * @since 2026-07-28
 */
public class ActivityCountEntityTest {

    /** 验证 Builder 能创建包含全部字段的次数配置实体。 */
    @Test
    public void test_builder_createsEntityWithAllFields() {
        ActivityCountEntity entity = ActivityCountEntity.builder()
                .activityCountId(10001L)
                .totalCount(10)
                .dayCount(3)
                .monthCount(5)
                .build();

        assertNotNull(entity);
        assertEquals(Long.valueOf(10001L), entity.getActivityCountId());
        assertEquals(Integer.valueOf(10), entity.getTotalCount());
        assertEquals(Integer.valueOf(3), entity.getDayCount());
        assertEquals(Integer.valueOf(5), entity.getMonthCount());
    }

    /** 验证日次数与月次数均不超过总次数。 */
    @Test
    public void test_dayCount_notExceedTotalCount() {
        ActivityCountEntity entity = ActivityCountEntity.builder()
                .totalCount(10)
                .dayCount(3)
                .monthCount(5)
                .build();

        assertTrue(entity.getDayCount() <= entity.getTotalCount());
        assertTrue(entity.getMonthCount() <= entity.getTotalCount());
    }

    /** 验证全参构造能正确赋值。 */
    @Test
    public void test_allArgsConstructor() {
        ActivityCountEntity entity = new ActivityCountEntity(10001L, 10, 3, 5);
        assertEquals(Long.valueOf(10001L), entity.getActivityCountId());
        assertEquals(Integer.valueOf(10), entity.getTotalCount());
    }
}
