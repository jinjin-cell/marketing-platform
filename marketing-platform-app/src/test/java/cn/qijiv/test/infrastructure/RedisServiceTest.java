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

/** Redis 服务集成测试：验证基础读写与删除能力。 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisServiceTest {

    /** Redis 服务接口，用于执行缓存读写操作。 */
    @Resource
    private IRedisService redisService;

    /** 验证 Redis 的写入、读取、存在性判断与删除全流程。 */
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
