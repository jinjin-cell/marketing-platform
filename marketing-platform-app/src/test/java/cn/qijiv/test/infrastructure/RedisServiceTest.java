package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisServiceTest {

    @Resource
    private IRedisService redisService;

    @Test
    public void test_setGetAndDelete() {
        String key = "test:marketing-platform:redis:" + UUID.randomUUID();

        try {
            redisService.setValue(key, "redis-ready", 1, TimeUnit.MINUTES);

            assertTrue(redisService.isExists(key));
            assertEquals("redis-ready", redisService.getValue(key));
            assertTrue(redisService.delete(key));
            assertFalse(redisService.isExists(key));
        } finally {
            redisService.delete(key);
        }
    }

}
