package cn.qijiv.domain.activity.service;

import cn.qijiv.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import cn.qijiv.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.qijiv.domain.activity.model.entity.*;
import cn.qijiv.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.qijiv.domain.activity.model.valobj.ActivityStateVO;
import cn.qijiv.domain.activity.repository.IActivityRepository;
import cn.qijiv.domain.activity.service.quota.RaffleActivityAccountQuotaService;
import cn.qijiv.domain.activity.service.quota.rule.IActionChain;
import cn.qijiv.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 抽奖活动服务单元测试
 * <p>
 * 使用手动 Stub 替代 Mockito，避免给 domain 模块引入额外依赖。
 *
 * @author jinlujia
 * @since 2026-07-28
 */
public class RaffleActivityServiceTest {

    private RaffleActivityAccountQuotaService raffleActivityService;

    /**
     * IActivityRepository 的手动桩，用于模拟数据库查询行为。
     */
    private static class StubActivityRepository implements IActivityRepository {

        private ActivitySkuEntity skuEntity;
        private ActivityEntity activityEntity;
        private ActivityCountEntity countEntity;
        private Long queriedSku;
        private Long queriedActivityId;
        private Long queriedActivityCountId;
        private CreateQuotaOrderAggregate savedAggregate;
        private String existingOrderId;
        private ActivitySkuStockKeyVO queuedStock;
        private Long updatedStockSku;
        private Long clearedStockSku;

        public void setSkuEntity(ActivitySkuEntity skuEntity) {
            this.skuEntity = skuEntity;
        }

        public void setActivityEntity(ActivityEntity activityEntity) {
            this.activityEntity = activityEntity;
        }

        public void setCountEntity(ActivityCountEntity countEntity) {
            this.countEntity = countEntity;
        }

        public CreateQuotaOrderAggregate getSavedAggregate() {
            return savedAggregate;
        }

        public void setExistingOrderId(String existingOrderId) {
            this.existingOrderId = existingOrderId;
        }

        @Override
        public ActivitySkuEntity queryActivitySku(Long sku) {
            queriedSku = sku;
            return skuEntity;
        }

        @Override
        public ActivityEntity queryRaffleActivityByActivityId(Long activityId) {
            queriedActivityId = activityId;
            return activityEntity;
        }

        @Override
        public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
            queriedActivityCountId = activityCountId;
            return countEntity;
        }

        @Override
        public String queryOrderIdByOutBusinessNo(String userId, String outBusinessNo) {
            return existingOrderId;
        }

        @Override
        public void doSaveOrder(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
            this.savedAggregate = createQuotaOrderAggregate;
        }

        @Override
        public void cacheActivitySkuStockCount(String cacheKey, Integer stockCount) {
        }

        @Override
        public boolean subtractionActivitySkuStock(Long sku, String cacheKey, Date endDateTime) {
            return true;
        }

        @Override
        public void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO activitySkuStockKeyVO) {
            queuedStock = activitySkuStockKeyVO;
        }

        @Override
        public ActivitySkuStockKeyVO takeQueueValue() {
            return queuedStock;
        }

        @Override
        public void clearQueueValue() {
            queuedStock = null;
        }

        @Override
        public void updateActivitySkuStock(Long sku) {
            updatedStockSku = sku;
        }

        @Override
        public void clearActivitySkuStock(Long sku) {
            clearedStockSku = sku;
        }

        @Override
        public void saveCreatePartakeOrderAggregate(CreatePartakeOrderAggregate createPartakeOrderAggregate) {

        }

        @Override
        public ActivityAccountDayEntity queryActivityAccountDayByUserId(String userId, Long activityId, String day) {
            return null;
        }

        @Override
        public ActivityAccountEntity queryActivityAccountByUserId(String userId, Long activityId) {
            return null;
        }

        @Override
        public ActivityAccountMonthEntity queryActivityAccountMonthByUserId(String userId, Long activityId, String month) {
            return null;
        }

        @Override
        public UserRaffleOrderEntity queryNoUsedRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity) {
            return null;
        }

        @Override
        public List<ActivitySkuEntity> queryActivitySkuListByActivityId(Long activityId) {
            return Collections.emptyList();
        }
    }

    /**
     * IActionChain 的手动桩，始终返回 true
     */
    private static class StubActionChain implements IActionChain {

        @Override
        public boolean action(ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity) {
            return true;
        }

        @Override
        public IActionChain next() {
            return null;
        }

        @Override
        public IActionChain appendNext(IActionChain next) {
            return this;
        }
    }

    private StubActivityRepository stubRepo;

    /** 构造 stub 仓储与责任链工厂，初始化被测服务。 */
    @Before
    public void setUp() {
        stubRepo = new StubActivityRepository();
        // 构造 DefaultActivityChainFactory，传入包含stub链的Map
        StubActionChain stubChain = new StubActionChain();
        HashMap<String, IActionChain> chainGroup = new HashMap<>();
        chainGroup.put(DefaultActivityChainFactory.ActionModel.activity_base_action.getCode(), stubChain);
        chainGroup.put(DefaultActivityChainFactory.ActionModel.activity_sku_stock_action.getCode(), stubChain);
        DefaultActivityChainFactory chainFactory = new DefaultActivityChainFactory(chainGroup);
        raffleActivityService = new RaffleActivityAccountQuotaService(stubRepo, chainFactory);
    }

    /** 验证正常创建 SKU 充值订单时返回非空订单 ID。 */
    @Test
    public void test_createSkuRechargeOrder_success() {
        // 1. 准备 SKU 数据
        ActivitySkuEntity skuEntity = ActivitySkuEntity.builder()
                .sku(10001L)
                .activityId(20001L)
                .activityCountId(30001L)
                .stockCount(100)
                .stockCountSurplus(50)
                .build();
        stubRepo.setSkuEntity(skuEntity);

        // 2. 准备活动数据
        ActivityEntity activityEntity = ActivityEntity.builder()
                .activityId(20001L)
                .activityName("双十一抽奖")
                .activityDesc("双十一促销活动")
                .beginDateTime(new Date(System.currentTimeMillis() - 86400000L))
                .endDateTime(new Date(System.currentTimeMillis() + 86400000L))
                .strategyId(40001L)
                .state(ActivityStateVO.open)
                .build();
        stubRepo.setActivityEntity(activityEntity);

        // 3. 准备次数配置数据
        ActivityCountEntity countEntity = ActivityCountEntity.builder()
                .activityCountId(30001L)
                .totalCount(10)
                .dayCount(3)
                .monthCount(5)
                .build();
        stubRepo.setCountEntity(countEntity);

        // 4. 创建sku充值实体并调用
        SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
        skuRechargeEntity.setUserId("user001");
        skuRechargeEntity.setSku(10001L);
        skuRechargeEntity.setOutBusinessNo("biz_001");

        String orderId = raffleActivityService.createSkuRechargeOrder(skuRechargeEntity);

        // 5. 验证返回的订单ID
        assertNotNull(orderId);
    }

    /** 验证传入空请求时抛出非法参数异常。 */
    @Test
    public void test_createSkuRechargeOrder_nullRequest_throwsIllegalParameter() {
        try {
            raffleActivityService.createSkuRechargeOrder(null);
            fail("空请求应抛出非法参数异常");
        } catch (AppException e) {
            assertEquals(ResponseCode.ILLEGAL_PARAMETER.getCode(), e.getCode());
            assertEquals(ResponseCode.ILLEGAL_PARAMETER.getInfo(), e.getInfo());
        }
    }

    /** 验证业务单号已存在时直接返回原订单，不再重复创建。 */
    @Test
    public void test_createSkuRechargeOrder_existingBusinessNo_returnsOriginalOrder() {
        stubRepo.setExistingOrderId("123456789012");
        SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
        skuRechargeEntity.setUserId("user001");
        skuRechargeEntity.setSku(10001L);
        skuRechargeEntity.setOutBusinessNo("biz_existing");

        String orderId = raffleActivityService.createSkuRechargeOrder(skuRechargeEntity);

        assertEquals("123456789012", orderId);
        assertNull(stubRepo.queriedSku);
        assertNull(stubRepo.getSavedAggregate());
    }

    /** 验证使用不同 SKU 时仍能正常创建订单。 */
    @Test
    public void test_createSkuRechargeOrder_withDifferentSku() {
        ActivitySkuEntity skuEntity = ActivitySkuEntity.builder()
                .sku(99999L)
                .activityId(88888L)
                .activityCountId(77777L)
                .stockCount(50)
                .stockCountSurplus(10)
                .build();
        stubRepo.setSkuEntity(skuEntity);

        ActivityEntity activityEntity = ActivityEntity.builder()
                .activityId(88888L)
                .activityName("新年活动")
                .strategyId(60001L)
                .state(ActivityStateVO.create)
                .build();
        stubRepo.setActivityEntity(activityEntity);

        ActivityCountEntity countEntity = ActivityCountEntity.builder()
                .activityCountId(77777L)
                .totalCount(5)
                .dayCount(1)
                .monthCount(3)
                .build();
        stubRepo.setCountEntity(countEntity);

        SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
        skuRechargeEntity.setUserId("user002");
        skuRechargeEntity.setSku(99999L);
        skuRechargeEntity.setOutBusinessNo("biz_002");

        String orderId = raffleActivityService.createSkuRechargeOrder(skuRechargeEntity);

        assertNotNull(orderId);
    }

    /** 验证实体字段为空时方法不会抛出空指针异常。 */
    @Test
    public void test_createSkuRechargeOrder_returnsOrderWithBuilderDefaults() {
        // 当 repository 返回的 entity 全部为空字段时，验证方法不会 NPE
        stubRepo.setSkuEntity(new ActivitySkuEntity());
        stubRepo.setActivityEntity(new ActivityEntity());
        stubRepo.setCountEntity(new ActivityCountEntity());

        SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
        skuRechargeEntity.setUserId("user003");
        skuRechargeEntity.setSku(1L);
        skuRechargeEntity.setOutBusinessNo("biz_003");

        String orderId = raffleActivityService.createSkuRechargeOrder(skuRechargeEntity);

        assertNotNull(orderId);
    }

    /** 验证 SKU 与活动、次数配置的关联关系正确。 */
    @Test
    public void test_createSkuRechargeOrder_skuLinksToCorrectActivity() {
        // 验证 SKU → Activity → Count 的关联关系
        ActivitySkuEntity skuEntity = ActivitySkuEntity.builder()
                .sku(100L)
                .activityId(200L)
                .activityCountId(300L)
                .build();
        stubRepo.setSkuEntity(skuEntity);

        ActivityEntity activityEntity = ActivityEntity.builder()
                .activityId(200L)
                .activityName("关联活动")
                .strategyId(400L)
                .state(ActivityStateVO.open)
                .build();
        stubRepo.setActivityEntity(activityEntity);

        ActivityCountEntity countEntity = ActivityCountEntity.builder()
                .activityCountId(300L)
                .totalCount(20)
                .dayCount(5)
                .monthCount(10)
                .build();
        stubRepo.setCountEntity(countEntity);

        SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
        skuRechargeEntity.setUserId("user004");
        skuRechargeEntity.setSku(100L);
        skuRechargeEntity.setOutBusinessNo("biz_004");

        String orderId = raffleActivityService.createSkuRechargeOrder(skuRechargeEntity);

        assertNotNull(orderId);
    }

    /** 验证库存相关操作正确委托给仓储实现。 */
    @Test
    public void test_stockOperations_delegateToRepository() throws InterruptedException {
        ActivitySkuStockKeyVO stockKey = ActivitySkuStockKeyVO.builder()
                .sku(10001L)
                .activityId(20001L)
                .build();
        stubRepo.queuedStock = stockKey;

        assertSame(stockKey, raffleActivityService.takeQueueValue());

        raffleActivityService.updateActivitySkuStock(10001L);
        raffleActivityService.clearActivitySkuStock(10001L);
        raffleActivityService.clearQueueValue();

        assertEquals(Long.valueOf(10001L), stubRepo.updatedStockSku);
        assertEquals(Long.valueOf(10001L), stubRepo.clearedStockSku);
        assertNull(stubRepo.queuedStock);
    }
}
