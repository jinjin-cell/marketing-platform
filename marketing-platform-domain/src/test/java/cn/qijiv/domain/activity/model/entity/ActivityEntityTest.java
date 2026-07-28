package cn.qijiv.domain.activity.model.entity;

import cn.qijiv.domain.activity.model.valobj.ActivityStateVO;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * 活动实体单元测试
 *
 * @author jinlujia
 * @since 2026-07-28
 */
public class ActivityEntityTest {

    @Test
    public void test_builder_createsEntityWithAllFields() {
        Date begin = new Date();
        Date end = new Date(System.currentTimeMillis() + 86400000L);
        ActivityEntity entity = ActivityEntity.builder()
                .activityId(10001L)
                .activityName("测试活动")
                .activityDesc("测试活动描述")
                .beginDateTime(begin)
                .endDateTime(end)
                .activityCountId(20001L)
                .strategyId(30001L)
                .state(ActivityStateVO.open)
                .build();

        assertNotNull(entity);
        assertEquals(Long.valueOf(10001L), entity.getActivityId());
        assertEquals("测试活动", entity.getActivityName());
        assertEquals("测试活动描述", entity.getActivityDesc());
        assertEquals(begin, entity.getBeginDateTime());
        assertEquals(end, entity.getEndDateTime());
        assertEquals(Long.valueOf(20001L), entity.getActivityCountId());
        assertEquals(Long.valueOf(30001L), entity.getStrategyId());
        assertEquals(ActivityStateVO.open, entity.getState());
    }

    @Test
    public void test_noArgsConstructor_createsEmptyEntity() {
        ActivityEntity entity = new ActivityEntity();
        assertNotNull(entity);
        assertNull(entity.getActivityId());
        assertNull(entity.getState());
    }

    @Test
    public void test_allArgsConstructor_createsEntity() {
        Date begin = new Date();
        Date end = new Date();
        ActivityEntity entity = new ActivityEntity(10001L, "测试", "描述", begin, end,
                20001L, 30001L, ActivityStateVO.create);
        assertEquals(Long.valueOf(10001L), entity.getActivityId());
        assertEquals(ActivityStateVO.create, entity.getState());
    }

    @Test
    public void test_setters_modifyFields() {
        ActivityEntity entity = new ActivityEntity();
        entity.setActivityId(10001L);
        entity.setActivityName("新活动");
        entity.setState(ActivityStateVO.close);

        assertEquals(Long.valueOf(10001L), entity.getActivityId());
        assertEquals("新活动", entity.getActivityName());
        assertEquals(ActivityStateVO.close, entity.getState());
    }
}
