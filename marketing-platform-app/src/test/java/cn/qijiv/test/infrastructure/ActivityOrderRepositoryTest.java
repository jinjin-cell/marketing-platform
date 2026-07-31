package cn.qijiv.test.infrastructure;

import cn.qijiv.domain.activity.model.aggregate.CreateOrderAggregate;
import cn.qijiv.domain.activity.model.entity.ActivityOrderEntity;
import cn.qijiv.domain.activity.model.valobj.OrderStateVO;
import cn.qijiv.infrastructure.persistent.repository.ActivityRepository;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ActivityOrderRepositoryTest {

    private static final String USER_ID = "order_flow_test_001";
    private static final Long ACTIVITY_ID = 100301L;

    @Resource
    private ActivityRepository activityRepository;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Before
    @After
    public void cleanTestData() {
        jdbcTemplate.update("DELETE FROM raffle_activity_order WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM raffle_activity_account WHERE user_id = ? AND activity_id = ?", USER_ID, ACTIVITY_ID);
    }

    @Test
    public void doSaveOrder_persistsOrdersAccumulatesQuotaAndRollsBackDuplicate() {
        activityRepository.doSaveOrder(createAggregate("100000000001", "order-flow-business-001", 10, 3, 5));
        activityRepository.doSaveOrder(createAggregate("100000000002", "order-flow-business-002", 2, 1, 1));

        assertEquals(Integer.valueOf(2), queryOrderCount());
        assertAccountQuota(12, 12, 4, 4, 6, 6);

        try {
            activityRepository.doSaveOrder(createAggregate("100000000003", "order-flow-business-001", 20, 20, 20));
            fail("重复外部业务单号应触发唯一索引异常");
        } catch (AppException e) {
            assertEquals(ResponseCode.INDEX_DUP.getCode(), e.getCode());
        }

        assertEquals(Integer.valueOf(2), queryOrderCount());
        assertAccountQuota(12, 12, 4, 4, 6, 6);
    }

    private CreateOrderAggregate createAggregate(String orderId, String outBusinessNo,
                                                   int totalCount, int dayCount, int monthCount) {
        ActivityOrderEntity order = ActivityOrderEntity.builder()
                .userId(USER_ID)
                .sku(901100000001L)
                .activityId(ACTIVITY_ID)
                .activityName("order flow integration test")
                .strategyId(100006L)
                .orderId(orderId)
                .orderTime(new Date())
                .totalCount(totalCount)
                .dayCount(dayCount)
                .monthCount(monthCount)
                .state(OrderStateVO.completed)
                .outBusinessNo(outBusinessNo)
                .build();
        return CreateOrderAggregate.builder()
                .userId(USER_ID)
                .activityId(ACTIVITY_ID)
                .totalCount(totalCount)
                .dayCount(dayCount)
                .monthCount(monthCount)
                .activityOrderEntity(order)
                .build();
    }

    private Integer queryOrderCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM raffle_activity_order WHERE user_id = ?",
                Integer.class,
                USER_ID
        );
    }

    private void assertAccountQuota(int totalCount, int totalCountSurplus,
                                    int dayCount, int dayCountSurplus,
                                    int monthCount, int monthCountSurplus) {
        Map<String, Object> account = jdbcTemplate.queryForMap(
                "SELECT total_count, total_count_surplus, day_count, day_count_surplus, " +
                        "month_count, month_count_surplus FROM raffle_activity_account " +
                        "WHERE user_id = ? AND activity_id = ?",
                USER_ID,
                ACTIVITY_ID
        );
        assertEquals(totalCount, ((Number) account.get("total_count")).intValue());
        assertEquals(totalCountSurplus, ((Number) account.get("total_count_surplus")).intValue());
        assertEquals(dayCount, ((Number) account.get("day_count")).intValue());
        assertEquals(dayCountSurplus, ((Number) account.get("day_count_surplus")).intValue());
        assertEquals(monthCount, ((Number) account.get("month_count")).intValue());
        assertEquals(monthCountSurplus, ((Number) account.get("month_count_surplus")).intValue());
    }
}
