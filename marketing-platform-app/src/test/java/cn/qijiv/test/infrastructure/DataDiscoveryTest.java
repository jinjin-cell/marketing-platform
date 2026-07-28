package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.persistent.dao.IRaffleActivityCountDao;
import cn.qijiv.infrastructure.persistent.dao.IRaffleActivityDao;
import cn.qijiv.infrastructure.persistent.dao.IRaffleActivitySkuDao;
import cn.qijiv.infrastructure.persistent.po.RaffleActivityCountPO;
import cn.qijiv.infrastructure.persistent.po.RaffleActivityPO;
import cn.qijiv.infrastructure.persistent.po.RaffleActivitySkuPO;
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

    @Resource
    private IRaffleActivitySkuDao skuDao;
    @Resource
    private IRaffleActivityDao activityDao;
    @Resource
    private IRaffleActivityCountDao countDao;

    @Test
    public void discoverSkuData() {
        Long[] testSkus = {901100000001L, 10001L, 1L, 1001L};
        for (Long sku : testSkus) {
            RaffleActivitySkuPO s = skuDao.queryRaffleActivitySkuBySku(sku);
            log.info("SKU {}: {}", sku, s == null ? "NOT FOUND" : s);
        }
    }

    @Test
    public void discoverActivityData() {
        Long[] testIds = {10001L, 1L, 1001L, 20001L};
        for (Long id : testIds) {
            RaffleActivityPO a = activityDao.queryRaffleActivityByActivityId(id);
            log.info("Activity {}: {}", id, a == null ? "NOT FOUND" : a.getActivityName());
        }
    }

    @Test
    public void discoverCountData() {
        Long[] testIds = {10001L, 1L, 1001L, 20001L};
        for (Long id : testIds) {
            RaffleActivityCountPO c = countDao.queryRaffleActivityCountByActivityCountId(id);
            log.info("Count {}: {}", id, c == null ? "NOT FOUND" : c.getTotalCount());
        }
    }
}
