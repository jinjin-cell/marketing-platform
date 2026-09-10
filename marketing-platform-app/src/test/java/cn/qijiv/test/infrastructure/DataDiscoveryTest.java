package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.dao.IRaffleActivityCountDao;
import cn.qijiv.infrastructure.dao.IRaffleActivityDao;
import cn.qijiv.infrastructure.dao.IRaffleActivitySkuDao;
import cn.qijiv.infrastructure.dao.po.RaffleActivityCountPO;
import cn.qijiv.infrastructure.dao.po.RaffleActivityPO;
import cn.qijiv.infrastructure.dao.po.RaffleActivitySkuPO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 数据发现测试 - 用于确认数据库中实际存在的数据ID
 *
 * @author jinlujia
 * @since 2026-07-28
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class DataDiscoveryTest {

    /** SKU DAO，用于探查 SKU 数据。 */
    @Resource
    private IRaffleActivitySkuDao skuDao;
    /** 活动 DAO，用于探查活动数据。 */
    @Resource
    private IRaffleActivityDao activityDao;
    /** 次数配置 DAO，用于探查次数配置数据。 */
    @Resource
    private IRaffleActivityCountDao countDao;

    /** 探查若干候选 sku 在数据库中是否存在并打印结果。 */
    @Test
    public void discoverSkuData() {
        Long[] testSkus = {901100000001L, 10001L, 1L, 1001L};
        for (Long sku : testSkus) {
            RaffleActivitySkuPO s = skuDao.queryRaffleActivitySkuBySku(sku);
            log.info("SKU {}: {}", sku, s == null ? "NOT FOUND" : s);
        }
    }

    /** 探查若干候选活动 ID 在数据库中是否存在并打印结果。 */
    @Test
    public void discoverActivityData() {
        Long[] testIds = {10001L, 1L, 1001L, 20001L};
        for (Long id : testIds) {
            RaffleActivityPO a = activityDao.queryRaffleActivityByActivityId(id);
            log.info("Activity {}: {}", id, a == null ? "NOT FOUND" : a.getActivityName());
        }
    }

    /** 探查若干候选次数配置 ID 在数据库中是否存在并打印结果。 */
    @Test
    public void discoverCountData() {
        Long[] testIds = {10001L, 1L, 1001L, 20001L};
        for (Long id : testIds) {
            RaffleActivityCountPO c = countDao.queryRaffleActivityCountByActivityCountId(id);
            log.info("Count {}: {}", id, c == null ? "NOT FOUND" : c.getTotalCount());
        }
    }
}
