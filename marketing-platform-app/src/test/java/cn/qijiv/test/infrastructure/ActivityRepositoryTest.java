package cn.qijiv.test.infrastructure;

import cn.qijiv.domain.activity.model.entity.ActivityCountEntity;
import cn.qijiv.domain.activity.model.entity.ActivityEntity;
import cn.qijiv.domain.activity.model.entity.ActivitySkuEntity;
import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import cn.qijiv.infrastructure.persistent.repository.ActivityRepository;
import cn.qijiv.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.sql.Timestamp;

import static org.junit.Assert.*;

/**
 * 活动仓库集成测试
 * <p>
 * 测试 ActivityRepository 的数据查询与 Redis 缓存逻辑。
 *
 * @author jinlujia
 * @since 2026-07-28
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ActivityRepositoryTest {

    @Resource
    private ActivityRepository activityRepository;

    @Resource
    private IRedisService redisService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    private static final Long TEST_SKU = 888777666L;
    private static final Long TEST_ACTIVITY_ID = 888777666L;
    private static final Long TEST_COUNT_ID = 888777666L;

    @Before
    public void setUp() {
        // 清空相关缓存
        redisService.delete(Constants.RedisKey.ACTIVITY_KEY + TEST_ACTIVITY_ID);
        redisService.delete(Constants.RedisKey.ACTIVITY_COUNT_KEY + TEST_COUNT_ID);

        // 插入测试数据
        jdbcTemplate.update(
                "INSERT INTO raffle_activity_sku (sku, activity_id, activity_count_id, stock_count, stock_count_surplus, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_SKU, TEST_ACTIVITY_ID, TEST_COUNT_ID, 200, 80
        );
        jdbcTemplate.update(
                "INSERT INTO raffle_activity (activity_id, activity_name, activity_desc, begin_date_time, end_date_time, strategy_id, state, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_ACTIVITY_ID, "测试活动", "测试描述",
                new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000),
                10001L, "open"
        );
        jdbcTemplate.update(
                "INSERT INTO raffle_activity_count (activity_count_id, total_count, day_count, month_count, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, NOW(), NOW())",
                TEST_COUNT_ID, 10, 3, 5
        );

        log.info("已插入测试数据: activityId={}, countId={}, sku={}", TEST_ACTIVITY_ID, TEST_COUNT_ID, TEST_SKU);
    }

    @After
    public void tearDown() {
        redisService.delete(Constants.RedisKey.ACTIVITY_KEY + TEST_ACTIVITY_ID);
        redisService.delete(Constants.RedisKey.ACTIVITY_COUNT_KEY + TEST_COUNT_ID);
        jdbcTemplate.update("DELETE FROM raffle_activity_sku WHERE sku = ?", TEST_SKU);
        jdbcTemplate.update("DELETE FROM raffle_activity WHERE activity_id = ?", TEST_ACTIVITY_ID);
        jdbcTemplate.update("DELETE FROM raffle_activity_count WHERE activity_count_id = ?", TEST_COUNT_ID);
        log.info("已清理测试数据");
    }

    // ==================== queryActivitySku ====================

    @Test
    public void test_queryActivitySku_exists() {
        ActivitySkuEntity entity = activityRepository.queryActivitySku(TEST_SKU);

        assertNotNull("SKU 实体不应为空", entity);
        assertEquals(TEST_SKU, entity.getSku());
        assertEquals(TEST_ACTIVITY_ID, entity.getActivityId());
        assertEquals(TEST_COUNT_ID, entity.getActivityCountId());
        assertEquals(Integer.valueOf(200), entity.getStockCount());
        assertEquals(Integer.valueOf(80), entity.getStockCountSurplus());

        log.info("查询SKU结果：{}", entity);
    }

    // ==================== queryRaffleActivityByActivityId ====================

    @Test
    public void test_queryRaffleActivityByActivityId_exists() {
        ActivityEntity entity = activityRepository.queryRaffleActivityByActivityId(TEST_ACTIVITY_ID);

        assertNotNull("活动实体不应为空", entity);
        assertEquals(TEST_ACTIVITY_ID, entity.getActivityId());
        assertEquals("测试活动", entity.getActivityName());
        assertNotNull("活动状态不应为空", entity.getState());
        assertEquals(Long.valueOf(10001L), entity.getStrategyId());

        log.info("查询活动结果：activityId={}, activityName={}, state={}, strategyId={}",
                entity.getActivityId(), entity.getActivityName(),
                entity.getState(), entity.getStrategyId());
    }

    @Test
    public void test_queryRaffleActivityByActivityId_cacheWorks() {
        String cacheKey = Constants.RedisKey.ACTIVITY_KEY + TEST_ACTIVITY_ID;

        // 确保缓存已清除
        redisService.delete(cacheKey);
        assertFalse("缓存清除后不应存在", redisService.isExists(cacheKey));

        // 第一次查询：从数据库加载并写入缓存
        ActivityEntity first = activityRepository.queryRaffleActivityByActivityId(TEST_ACTIVITY_ID);
        assertNotNull("第一次查询不应为空", first);
        assertTrue("第一次查询后应有缓存", redisService.isExists(cacheKey));

        // 第二次查询：应从缓存获取
        ActivityEntity second = activityRepository.queryRaffleActivityByActivityId(TEST_ACTIVITY_ID);
        assertNotNull("第二次查询不应为空", second);
        assertEquals(first.getActivityId(), second.getActivityId());
        assertEquals(first.getActivityName(), second.getActivityName());
        assertEquals(first.getState(), second.getState());
        assertEquals(first.getStrategyId(), second.getStrategyId());

        log.info("活动缓存测试通过：两次查询结果一致");
    }

    // ==================== queryRaffleActivityCountByActivityCountId ====================

    @Test
    public void test_queryRaffleActivityCountByActivityCountId_exists() {
        ActivityCountEntity entity = activityRepository.queryRaffleActivityCountByActivityCountId(TEST_COUNT_ID);

        assertNotNull("次数配置实体不应为空", entity);
        assertEquals(TEST_COUNT_ID, entity.getActivityCountId());
        assertEquals(Integer.valueOf(10), entity.getTotalCount());
        assertEquals(Integer.valueOf(3), entity.getDayCount());
        assertEquals(Integer.valueOf(5), entity.getMonthCount());

        log.info("查询次数配置结果：activityCountId={}, total={}, day={}, month={}",
                entity.getActivityCountId(), entity.getTotalCount(),
                entity.getDayCount(), entity.getMonthCount());
    }

    @Test
    public void test_queryRaffleActivityCountByActivityCountId_cacheWorks() {
        String cacheKey = Constants.RedisKey.ACTIVITY_COUNT_KEY + TEST_COUNT_ID;

        // 确保缓存已清除
        redisService.delete(cacheKey);
        assertFalse("缓存清除后不应存在", redisService.isExists(cacheKey));

        // 第一次查询：从数据库加载并写入缓存
        ActivityCountEntity first = activityRepository.queryRaffleActivityCountByActivityCountId(TEST_COUNT_ID);
        assertNotNull("第一次查询不应为空", first);
        assertTrue("第一次查询后应有缓存", redisService.isExists(cacheKey));

        // 第二次查询：应从缓存获取
        ActivityCountEntity second = activityRepository.queryRaffleActivityCountByActivityCountId(TEST_COUNT_ID);
        assertNotNull("第二次查询不应为空", second);
        assertEquals(first.getActivityCountId(), second.getActivityCountId());
        assertEquals(first.getTotalCount(), second.getTotalCount());
        assertEquals(first.getDayCount(), second.getDayCount());
        assertEquals(first.getMonthCount(), second.getMonthCount());

        log.info("次数配置缓存测试通过：两次查询结果一致");
    }
}
