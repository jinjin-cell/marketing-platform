package cn.qijiv.domain.activity.model.valobj;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 活动状态枚举单元测试
 *
 * @author jinlujia
 * @since 2026-07-28
 */
public class ActivityStateVOTest {

    @Test
    public void test_enum_values_exist() {
        ActivityStateVO[] values = ActivityStateVO.values();
        assertEquals(3, values.length);
    }

    @Test
    public void test_create_state() {
        assertEquals("create", ActivityStateVO.create.getCode());
        assertEquals("创建", ActivityStateVO.create.getDesc());
    }

    @Test
    public void test_open_state() {
        assertEquals("open", ActivityStateVO.open.getCode());
        assertEquals("开启", ActivityStateVO.open.getDesc());
    }

    @Test
    public void test_close_state() {
        assertEquals("close", ActivityStateVO.close.getCode());
        assertEquals("关闭", ActivityStateVO.close.getDesc());
    }

    @Test
    public void test_valueOf_returnsCorrectEnum() {
        assertEquals(ActivityStateVO.create, ActivityStateVO.valueOf("create"));
        assertEquals(ActivityStateVO.open, ActivityStateVO.valueOf("open"));
        assertEquals(ActivityStateVO.close, ActivityStateVO.valueOf("close"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_valueOf_invalidValue_throwsException() {
        ActivityStateVO.valueOf("invalid");
    }
}
