package cn.qijiv.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/** 冒烟测试：验证 Spring 上下文能否正常启动。 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

    /** 打印一条日志，确认测试环境可正常执行。 */
    @Test
    public void test() {
        log.info("测试完成");
    }

}
