package cn.qijiv.domain.rebate.service;

import cn.qijiv.domain.rebate.event.SendRebateMessageEvent;
import cn.qijiv.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import cn.qijiv.domain.rebate.model.entity.BehaviorEntity;
import cn.qijiv.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.qijiv.domain.rebate.model.valobj.BehaviorTypeVO;
import cn.qijiv.domain.rebate.model.valobj.DailyBehaviorRebateVO;
import cn.qijiv.domain.rebate.model.valobj.TaskStateVO;
import cn.qijiv.domain.rebate.repository.IBehaviorRebateRepository;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 行为返利服务单元测试
 *
 * @author qijiv
 * @since 2026-08-26
 */
public class BehaviorRebateServiceTest {

    private BehaviorRebateService behaviorRebateService;
    private StubBehaviorRebateRepository repository;

    @Before
    public void setUp() throws Exception {
        repository = new StubBehaviorRebateRepository();
        SendRebateMessageEvent messageEvent = new SendRebateMessageEvent();
        setField(messageEvent, "topic", "send.rebate");

        behaviorRebateService = new BehaviorRebateService();
        setField(behaviorRebateService, "behaviorRebateRepository", repository);
        setField(behaviorRebateService, "sendRebateMessageEvent", messageEvent);
    }

    @Test
    public void createOrder_noEnabledConfig_returnsEmptyList() {
        repository.rebateConfigs = Collections.emptyList();

        List<String> orderIds = behaviorRebateService.createOrder(BehaviorEntity.builder()
                .userId("user001")
                .behaviorTypeVO(BehaviorTypeVO.SIGN)
                .outBusinessNo("2026-08-26")
                .build());

        assertTrue(orderIds.isEmpty());
        assertFalse(repository.saveCalled);
    }

    @Test
    public void createOrder_enabledConfig_buildsOrderAndMessage() {
        repository.rebateConfigs = Collections.singletonList(DailyBehaviorRebateVO.builder()
                .behaviorType("sign")
                .rebateDesc("签到返利-sku额度")
                .rebateType("sku")
                .rebateConfig("9011")
                .build());

        List<String> orderIds = behaviorRebateService.createOrder(BehaviorEntity.builder()
                .userId("user001")
                .behaviorTypeVO(BehaviorTypeVO.SIGN)
                .outBusinessNo("2026-08-26")
                .build());

        assertEquals(1, orderIds.size());
        assertTrue(orderIds.get(0).matches("\\d{12}"));
        assertEquals("user001", repository.savedUserId);
        assertEquals(1, repository.savedAggregates.size());

        BehaviorRebateAggregate aggregate = repository.savedAggregates.get(0);
        BehaviorRebateOrderEntity order = aggregate.getBehaviorRebateOrderEntity();
        assertEquals(orderIds.get(0), order.getOrderId());
        assertEquals("sign", order.getBehaviorType());
        assertEquals("sku", order.getRebateType());
        assertEquals("user001_sku_2026-08-26", order.getBizId());

        assertEquals("send.rebate", aggregate.getTaskEntity().getTopic());
        assertEquals(TaskStateVO.create, aggregate.getTaskEntity().getState());
        SendRebateMessageEvent.RebateMessage message = aggregate.getTaskEntity().getMessage().getData();
        assertEquals("签到返利-sku额度", message.getRebateDesc());
        assertEquals("sku", message.getRebateType());
        assertEquals("9011", message.getRebateConfig());
        assertEquals("user001_sku_2026-08-26", message.getBizId());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class StubBehaviorRebateRepository implements IBehaviorRebateRepository {

        private List<DailyBehaviorRebateVO> rebateConfigs = new ArrayList<>();
        private String savedUserId;
        private List<BehaviorRebateAggregate> savedAggregates;
        private boolean saveCalled;

        @Override
        public List<DailyBehaviorRebateVO> queryDailyBehaviorRebateConfig(BehaviorTypeVO behaviorTypeVO) {
            return rebateConfigs;
        }

        @Override
        public void saveUserRebateRecord(String userId, List<BehaviorRebateAggregate> behaviorRebateAggregates) {
            saveCalled = true;
            savedUserId = userId;
            savedAggregates = behaviorRebateAggregates;
        }
    }

}
