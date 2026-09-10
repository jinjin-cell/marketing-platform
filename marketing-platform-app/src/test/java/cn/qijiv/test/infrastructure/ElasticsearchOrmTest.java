package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.dao.IRaffleActivityDao;
import cn.qijiv.infrastructure.dao.po.RaffleActivityPO;
import cn.qijiv.infrastructure.elasticsearch.IElasticSearchUserRaffleOrderDao;
import cn.qijiv.infrastructure.elasticsearch.po.UserRaffleOrderPO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/** 验证 MySQL/ShardingSphere 与 Elasticsearch 两套 MyBatis 数据源可同时使用。 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticsearchOrmTest {

    @Resource
    private IRaffleActivityDao raffleActivityDao;

    @Resource
    private IElasticSearchUserRaffleOrderDao elasticSearchUserRaffleOrderDao;

    @Test
    public void shouldQueryMysqlAndElasticsearchThroughTheirOwnMappers() {
        RaffleActivityPO activity = raffleActivityDao.queryRaffleActivityByActivityId(100301L);
        assertNotNull("MySQL Mapper 应通过 ShardingSphere 查询到活动", activity);

        List<UserRaffleOrderPO> orders =
                elasticSearchUserRaffleOrderDao.queryUserRaffleOrderListByUserId("xiaofuge");
        assertFalse("Elasticsearch Mapper 应查询到 Canal 已同步的订单", orders.isEmpty());
        assertEquals("xiaofuge", orders.get(0).getUserId());
        assertNotNull("ES 下划线字段应映射到 orderId", orders.get(0).getOrderId());
        assertNotNull("ES 的 _create_time 应映射到 createTime", orders.get(0).getCreateTime());
    }
}
