package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.dao.IRaffleActivitySkuDao;
import cn.qijiv.infrastructure.dao.po.RaffleActivitySkuPO;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.math.BigDecimal;

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

    /** SKU DAO，用于按 sku 查询活动 SKU 数据。 */
    @Resource
    private IRaffleActivitySkuDao raffleActivitySkuDao;

    /** JdbcTemplate，用于直接操作数据库插入/清理测试数据。 */
    @Resource
    private JdbcTemplate jdbcTemplate;

    private static final Long TEST_SKU = 999888777L;
    private static final Long TEST_ACTIVITY_ID = 999888777L;
    private static final Long TEST_COUNT_ID = 999888777L;

    /** 在每个测试方法前插入测试 SKU 数据。 */
    @Before
    public void setUp() {
        // 插入测试数据到 raffle_activity_sku 表
        jdbcTemplate.update(
                "INSERT INTO raffle_activity_sku (sku, activity_id, activity_count_id, stock_count, stock_count_surplus, product_amount, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_SKU, TEST_ACTIVITY_ID, TEST_COUNT_ID, 100, 50, new BigDecimal("8.80")
        );
        log.info("已插入测试SKU数据: sku={}", TEST_SKU);
    }

    /** 在每个测试方法后清理测试 SKU 数据。 */
    @After
    public void tearDown() {
        jdbcTemplate.update("DELETE FROM raffle_activity_sku WHERE sku = ?", TEST_SKU);
        log.info("已清理测试SKU数据: sku={}", TEST_SKU);
    }

    /** 验证存在指定 sku 时能正确查询出完整 SKU 数据。 */
    @Test
    public void test_queryRaffleActivitySkuBySku_exists() {
        RaffleActivitySkuPO sku = raffleActivitySkuDao.queryRaffleActivitySkuBySku(TEST_SKU);

        assertNotNull("SKU 查询结果不应为空", sku);
        assertEquals(TEST_SKU, sku.getSku());
        assertEquals(TEST_ACTIVITY_ID, sku.getActivityId());
        assertEquals(TEST_COUNT_ID, sku.getActivityCountId());
        assertEquals(Integer.valueOf(100), sku.getStockCount());
        assertEquals(Integer.valueOf(50), sku.getStockCountSurplus());
        assertEquals(0, new BigDecimal("8.80").compareTo(sku.getProductAmount()));

        log.info("查询结果：sku={}, activityId={}, activityCountId={}, stockCount={}, stockCountSurplus={}",
                sku.getSku(), sku.getActivityId(), sku.getActivityCountId(),
                sku.getStockCount(), sku.getStockCountSurplus());
    }

    /** 验证不存在的 sku 查询返回 null。 */
    @Test
    public void test_queryRaffleActivitySkuBySku_notExists() {
        RaffleActivitySkuPO sku = raffleActivitySkuDao.queryRaffleActivitySkuBySku(-1L);
        assertNull("不存在的 SKU 应返回 null", sku);
    }
}
