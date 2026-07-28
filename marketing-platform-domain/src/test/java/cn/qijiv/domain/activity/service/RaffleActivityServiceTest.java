package cn.qijiv.domain.activity.service;

import cn.qijiv.domain.activity.model.entity.*;
import cn.qijiv.domain.activity.model.valobj.ActivityStateVO;
import cn.qijiv.domain.activity.repository.IActivityRepository;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

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

    private RaffleActivityService raffleActivityService;

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

        public void setSkuEntity(ActivitySkuEntity skuEntity) {
            this.skuEntity = skuEntity;
        }

        public void setActivityEntity(ActivityEntity activityEntity) {
            this.activityEntity = activityEntity;
        }

        public void setCountEntity(ActivityCountEntity countEntity) {
            this.countEntity = countEntity;
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
    }

    private StubActivityRepository stubRepo;

    @Before
    public void setUp() {
        stubRepo = new StubActivityRepository();
        raffleActivityService = new RaffleActivityService(stubRepo);
    }

    @Test
    public void test_createRaffleActivityOrder_success() {
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

        // 4. 创建购物车实体并调用
        ActivityShopCartEntity cartEntity = ActivityShopCartEntity.builder()
                .userId("user001")
                .sku(10001L)
                .build();

        ActivityOrderEntity order = raffleActivityService.createRaffleActivityOrder(cartEntity);

        // 5. 验证返回的订单实体
        assertNotNull(order);
        // 验证通过 SKU 查询活动，再查询次数配置的链路走通
    }

    @Test
    public void test_createRaffleActivityOrder_withDifferentSku() {
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

        ActivityShopCartEntity cartEntity = ActivityShopCartEntity.builder()
                .userId("user002")
                .sku(99999L)
                .build();

        ActivityOrderEntity order = raffleActivityService.createRaffleActivityOrder(cartEntity);

        assertNotNull(order);
    }

    @Test
    public void test_createRaffleActivityOrder_returnsOrderWithBuilderDefaults() {
        // 当 repository 返回的 entity 全部为空字段时，验证方法不会 NPE
        stubRepo.setSkuEntity(new ActivitySkuEntity());
        stubRepo.setActivityEntity(new ActivityEntity());
        stubRepo.setCountEntity(new ActivityCountEntity());

        ActivityShopCartEntity cartEntity = ActivityShopCartEntity.builder()
                .userId("user003")
                .sku(1L)
                .build();

        ActivityOrderEntity order = raffleActivityService.createRaffleActivityOrder(cartEntity);

        assertNotNull(order);
    }

    @Test
    public void test_createRaffleActivityOrder_skuLinksToCorrectActivity() {
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

        ActivityShopCartEntity cartEntity = ActivityShopCartEntity.builder()
                .userId("user004")
                .sku(100L)
                .build();

        ActivityOrderEntity order = raffleActivityService.createRaffleActivityOrder(cartEntity);

        assertNotNull(order);
        assertEquals(Long.valueOf(100L), stubRepo.queriedSku);
        assertEquals(Long.valueOf(200L), stubRepo.queriedActivityId);
        assertEquals(Long.valueOf(300L), stubRepo.queriedActivityCountId);
    }
}
