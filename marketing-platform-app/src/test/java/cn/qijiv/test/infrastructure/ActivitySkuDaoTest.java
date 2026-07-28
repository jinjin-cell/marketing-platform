package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.persistent.dao.IRaffleActivitySkuDao;
import cn.qijiv.infrastructure.persistent.po.RaffleActivitySkuPO;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import static org.junit.Assert.*;

/**
 * 抽奖活动SKU DAO 集成测试
 *
 * @author jinlujia
 * @since 2026-07-28
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ActivitySkuDaoTest {

    @Resource
    private IRaffleActivitySkuDao raffleActivitySkuDao;

    @Resource
    private JdbcTemplate jdbcTemplate;

    private static final Long TEST_SKU = 999888777L;
    private static final Long TEST_ACTIVITY_ID = 999888777L;
    private static final Long TEST_COUNT_ID = 999888777L;

    @Before
    public void setUp() {
        // 插入测试数据到 raffle_activity_sku 表
        jdbcTemplate.update(
                "INSERT INTO raffle_activity_sku (sku, activity_id, activity_count_id, stock_count, stock_count_surplus, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_SKU, TEST_ACTIVITY_ID, TEST_COUNT_ID, 100, 50
        );
        log.info("已插入测试SKU数据: sku={}", TEST_SKU);
    }

    @After
    public void tearDown() {
        jdbcTemplate.update("DELETE FROM raffle_activity_sku WHERE sku = ?", TEST_SKU);
        log.info("已清理测试SKU数据: sku={}", TEST_SKU);
    }

    @Test
    public void test_queryRaffleActivitySkuBySku_exists() {
        RaffleActivitySkuPO sku = raffleActivitySkuDao.queryRaffleActivitySkuBySku(TEST_SKU);

        assertNotNull("SKU 查询结果不应为空", sku);
        assertEquals(TEST_SKU, sku.getSku());
        assertEquals(TEST_ACTIVITY_ID, sku.getActivityId());
        assertEquals(TEST_COUNT_ID, sku.getActivityCountId());
        assertEquals(Integer.valueOf(100), sku.getStockCount());
        assertEquals(Integer.valueOf(50), sku.getStockCountSurplus());

        log.info("查询结果：sku={}, activityId={}, activityCountId={}, stockCount={}, stockCountSurplus={}",
                sku.getSku(), sku.getActivityId(), sku.getActivityCountId(),
                sku.getStockCount(), sku.getStockCountSurplus());
    }

    @Test
    public void test_queryRaffleActivitySkuBySku_notExists() {
        RaffleActivitySkuPO sku = raffleActivitySkuDao.queryRaffleActivitySkuBySku(-1L);
        assertNull("不存在的 SKU 应返回 null", sku);
    }
}
