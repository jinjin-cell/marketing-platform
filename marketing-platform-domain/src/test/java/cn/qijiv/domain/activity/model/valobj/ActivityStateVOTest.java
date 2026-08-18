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

    /** 验证枚举共包含 3 个取值。 */
    @Test
    public void test_enum_values_exist() {
        ActivityStateVO[] values = ActivityStateVO.values();
        assertEquals(3, values.length);
    }

    /** 验证 create（创建）状态的编码与描述。 */
    @Test
    public void test_create_state() {
        assertEquals("create", ActivityStateVO.create.getCode());
        assertEquals("创建", ActivityStateVO.create.getDesc());
    }

    /** 验证 open（开启）状态的编码与描述。 */
    @Test
    public void test_open_state() {
        assertEquals("open", ActivityStateVO.open.getCode());
        assertEquals("开启", ActivityStateVO.open.getDesc());
    }

    /** 验证 close（关闭）状态的编码与描述。 */
    @Test
    public void test_close_state() {
        assertEquals("close", ActivityStateVO.close.getCode());
        assertEquals("关闭", ActivityStateVO.close.getDesc());
    }

    /** 验证 valueOf 能根据编码解析出正确的枚举。 */
    @Test
    public void test_valueOf_returnsCorrectEnum() {
        assertEquals(ActivityStateVO.create, ActivityStateVO.valueOf("create"));
        assertEquals(ActivityStateVO.open, ActivityStateVO.valueOf("open"));
        assertEquals(ActivityStateVO.close, ActivityStateVO.valueOf("close"));
    }

    /** 验证传入非法编码时 valueOf 抛出 IllegalArgumentException。 */
    @Test(expected = IllegalArgumentException.class)
    public void test_valueOf_invalidValue_throwsException() {
        ActivityStateVO.valueOf("invalid");
    }
}
