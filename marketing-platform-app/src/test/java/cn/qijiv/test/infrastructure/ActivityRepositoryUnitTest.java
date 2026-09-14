package cn.qijiv.test.infrastructure;

import cn.qijiv.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.qijiv.domain.activity.model.entity.ActivityCountEntity;
import cn.qijiv.domain.activity.model.entity.ActivityEntity;
import cn.qijiv.domain.activity.model.entity.ActivityOrderEntity;
import cn.qijiv.domain.activity.model.entity.ActivitySkuEntity;
import cn.qijiv.domain.activity.model.entity.SkuRechargeEntity;
import cn.qijiv.domain.activity.model.entity.UnpaidActivityOrderEntity;
import cn.qijiv.domain.activity.model.valobj.ActivityStateVO;
import cn.qijiv.domain.activity.model.valobj.OrderStateVO;
import cn.qijiv.infrastructure.dao.IRaffleActivityAccountDao;
import cn.qijiv.infrastructure.dao.IRaffleActivityCountDao;
import cn.qijiv.infrastructure.dao.IRaffleActivityDao;
import cn.qijiv.infrastructure.dao.IRaffleActivityOrderDao;
import cn.qijiv.infrastructure.dao.IRaffleActivitySkuDao;
import cn.qijiv.infrastructure.db.IDBRouterStrategy;
import cn.qijiv.infrastructure.dao.po.RaffleActivityAccountPO;
import cn.qijiv.infrastructure.dao.po.RaffleActivityCountPO;
import cn.qijiv.infrastructure.dao.po.RaffleActivityOrderPO;
import cn.qijiv.infrastructure.dao.po.RaffleActivityPO;
import cn.qijiv.infrastructure.dao.po.RaffleActivitySkuPO;
import cn.qijiv.infrastructure.redis.IRedisService;
import cn.qijiv.infrastructure.redis.OrderBusinessNoBloomFilter;
import cn.qijiv.infrastructure.adapter.repository.ActivityRepository;
import org.redisson.api.RLock;
import cn.qijiv.types.common.Constants;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 活动仓储单元测试：使用 Mock 验证查询缓存、订单保存与额度累计逻辑。 */
@RunWith(MockitoJUnitRunner.class)
public class ActivityRepositoryUnitTest {

    /** Mock 的 Redis 服务，用于模拟缓存读写。 */
    @Mock
    private IRedisService redisService;
    /** Mock 的活动 DAO，用于查询活动数据。 */
    @Mock
    private IRaffleActivityDao raffleActivityDao;
    /** Mock 的 SKU DAO，用于查询 SKU 数据。 */
    @Mock
    private IRaffleActivitySkuDao raffleActivitySkuDao;
    /** Mock 的次数配置 DAO，用于查询次数配置数据。 */
    @Mock
    private IRaffleActivityCountDao raffleActivityCountDao;
    /** Mock 的订单 DAO，用于插入与查询订单。 */
    @Mock
    private IRaffleActivityOrderDao raffleActivityOrderDao;
    /** Mock 的账户 DAO，用于更新或创建账户额度。 */
    @Mock
    private IRaffleActivityAccountDao raffleActivityAccountDao;
    /** Mock 的事务模板，用于控制事务回滚。 */
    @Mock
    private TransactionTemplate transactionTemplate;
    /** Mock 的事务状态，用于验证回滚标记。 */
    @Mock
    private TransactionStatus transactionStatus;
    /** Mock 的分库路由策略，用于验证路由与清理。 */
    @Mock
    private IDBRouterStrategy dbRouter;
    /** Mock 的订单号布隆过滤器，用于验证去重查询。 */
    @Mock
    private OrderBusinessNoBloomFilter orderBloomFilter;
    /** Mock 的分布式可重入锁，用于验证保存订单时的加锁路径。 */
    @Mock
    private RLock activityLock;

    /** 被测的活动仓储，由 Mock 依赖注入构建。 */
    @InjectMocks
    private ActivityRepository activityRepository;

    /** 让事务模板直接执行回调，以便验证事务内的行为。 */
    @Before
    public void executeTransactionCallbacks() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
    }

    /** 验证查询已存在 SKU 时能完整映射所有字段。 */
    @Test
    public void queryActivitySku_existingSku_mapsAllFields() {
        RaffleActivitySkuPO skuPO = new RaffleActivitySkuPO();
        skuPO.setSku(10001L);
        skuPO.setActivityId(20001L);
        skuPO.setActivityCountId(30001L);
        skuPO.setStockCount(100);
        skuPO.setStockCountSurplus(40);
        skuPO.setProductAmount(new BigDecimal("12.50"));
        when(raffleActivitySkuDao.queryRaffleActivitySkuBySku(10001L)).thenReturn(skuPO);

        ActivitySkuEntity result = activityRepository.queryActivitySku(10001L);

        assertEquals(Long.valueOf(10001L), result.getSku());
        assertEquals(Long.valueOf(20001L), result.getActivityId());
        assertEquals(Long.valueOf(30001L), result.getActivityCountId());
        assertEquals(Integer.valueOf(100), result.getStockCount());
        assertEquals(Integer.valueOf(40), result.getStockCountSurplus());
        assertEquals(new BigDecimal("12.50"), result.getProductAmount());
        verify(raffleActivitySkuDao).queryRaffleActivitySkuBySku(10001L);
    }

    /** 活动 SKU 库存只能原子初始化，重复装配不能覆盖 Redis 中已经扣减的值。 */
    @Test
    public void cacheActivitySkuStockCount_usesSetIfAbsent() {
        activityRepository.cacheActivitySkuStockCount("activity-stock", 40);

        verify(redisService).setAtomicLongIfAbsent("activity-stock", 40);
        verify(redisService, never()).setAtomicLong("activity-stock", 40);
    }

    /** Redis 库存缺失时从数据库快照懒初始化，然后再执行本次原子扣减。 */
    @Test
    public void subtractionActivitySkuStock_missingCache_initializesAtomically() {
        RaffleActivitySkuPO skuPO = new RaffleActivitySkuPO();
        skuPO.setSku(10001L);
        skuPO.setStockCountSurplus(40);
        when(redisService.isExists("activity-stock")).thenReturn(false);
        when(raffleActivitySkuDao.queryRaffleActivitySkuBySku(10001L)).thenReturn(skuPO);
        when(redisService.decr("activity-stock")).thenReturn(39L);
        when(redisService.setNx(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        boolean result = activityRepository.subtractionActivitySkuStock(
                10001L, "activity-stock", new Date(System.currentTimeMillis() + 60_000L));

        assertTrue(result);
        verify(redisService).setAtomicLongIfAbsent("activity-stock", 40);
        verify(redisService).decr("activity-stock");
    }

    /** 验证活动查询缓存命中时直接返回缓存，不再访问数据库。 */
    @Test
    public void queryRaffleActivity_cacheHit_returnsCachedEntityWithoutDatabaseQuery() {
        String cacheKey = Constants.RedisKey.ACTIVITY_KEY + 20001L;
        ActivityEntity cached = ActivityEntity.builder()
                .activityId(20001L)
                .activityName("cached activity")
                .build();
        when(redisService.getValue(cacheKey)).thenReturn(cached);

        ActivityEntity result = activityRepository.queryRaffleActivityByActivityId(20001L);

        assertSame(cached, result);
        verifyNoInteractions(raffleActivityDao);
        verify(redisService, never()).setValue(cacheKey, cached);
    }

    /** 验证活动查询缓存未命中时映射数据库实体并写入缓存。 */
    @Test
    public void queryRaffleActivity_cacheMiss_mapsDatabaseEntityAndCachesIt() {
        String cacheKey = Constants.RedisKey.ACTIVITY_KEY + 20001L;
        Date beginTime = new Date(1000L);
        Date endTime = new Date(2000L);
        RaffleActivityPO activityPO = new RaffleActivityPO();
        activityPO.setActivityId(20001L);
        activityPO.setActivityName("raffle activity");
        activityPO.setActivityDesc("description");
        activityPO.setBeginDateTime(beginTime);
        activityPO.setEndDateTime(endTime);
        activityPO.setStrategyId(40001L);
        activityPO.setState("open");
        when(redisService.getValue(cacheKey)).thenReturn(null);
        when(raffleActivityDao.queryRaffleActivityByActivityId(20001L)).thenReturn(activityPO);

        ActivityEntity result = activityRepository.queryRaffleActivityByActivityId(20001L);

        assertEquals(Long.valueOf(20001L), result.getActivityId());
        assertEquals("raffle activity", result.getActivityName());
        assertEquals("description", result.getActivityDesc());
        assertEquals(beginTime, result.getBeginDateTime());
        assertEquals(endTime, result.getEndDateTime());
        assertEquals(Long.valueOf(40001L), result.getStrategyId());
        assertEquals(ActivityStateVO.open, result.getState());
        verify(raffleActivityDao).queryRaffleActivityByActivityId(20001L);
        verify(redisService).setValue(cacheKey, result);
    }

    /** 验证次数配置查询缓存命中时直接返回缓存，不再访问数据库。 */
    @Test
    public void queryRaffleActivityCount_cacheHit_returnsCachedEntityWithoutDatabaseQuery() {
        String cacheKey = Constants.RedisKey.ACTIVITY_COUNT_KEY + 30001L;
        ActivityCountEntity cached = ActivityCountEntity.builder()
                .activityCountId(30001L)
                .totalCount(10)
                .build();
        when(redisService.getValue(cacheKey)).thenReturn(cached);

        ActivityCountEntity result = activityRepository.queryRaffleActivityCountByActivityCountId(30001L);

        assertSame(cached, result);
        verifyNoInteractions(raffleActivityCountDao);
        verify(redisService, never()).setValue(cacheKey, cached);
    }

    /** 验证次数配置查询缓存未命中时映射数据库实体并写入缓存。 */
    @Test
    public void queryRaffleActivityCount_cacheMiss_mapsDatabaseEntityAndCachesIt() {
        String cacheKey = Constants.RedisKey.ACTIVITY_COUNT_KEY + 30001L;
        RaffleActivityCountPO countPO = new RaffleActivityCountPO();
        countPO.setActivityCountId(30001L);
        countPO.setTotalCount(10);
        countPO.setDayCount(3);
        countPO.setMonthCount(5);
        when(redisService.getValue(cacheKey)).thenReturn(null);
        when(raffleActivityCountDao.queryRaffleActivityCountByActivityCountId(30001L)).thenReturn(countPO);

        ActivityCountEntity result = activityRepository.queryRaffleActivityCountByActivityCountId(30001L);

        assertEquals(Long.valueOf(30001L), result.getActivityCountId());
        assertEquals(Integer.valueOf(10), result.getTotalCount());
        assertEquals(Integer.valueOf(3), result.getDayCount());
        assertEquals(Integer.valueOf(5), result.getMonthCount());
        verify(raffleActivityCountDao).queryRaffleActivityCountByActivityCountId(30001L);
        verify(redisService).setValue(cacheKey, result);
    }

    /** 验证账户已存在时仅插入订单并更新账户额度，不重复创建账户。 */
    @Test
    public void doSaveOrder_existingAccount_insertsOrderAndAddsQuota() {
        mockAccountLock();
        CreateQuotaOrderAggregate aggregate = createOrderAggregate();
        when(raffleActivityAccountDao.updateAccountQuota(any(RaffleActivityAccountPO.class))).thenReturn(1);
        when(raffleActivityAccountDao.queryActivityAccountByUserId(any(RaffleActivityAccountPO.class))).thenReturn(new RaffleActivityAccountPO());

        activityRepository.doSaveOrder(aggregate);

        ArgumentCaptor<RaffleActivityOrderPO> orderCaptor = ArgumentCaptor.forClass(RaffleActivityOrderPO.class);
        ArgumentCaptor<RaffleActivityAccountPO> accountCaptor = ArgumentCaptor.forClass(RaffleActivityAccountPO.class);
        verify(dbRouter).doRouter("user001");
        verify(orderBloomFilter).add("user001", "business001");
        verify(raffleActivityOrderDao).insert(orderCaptor.capture());
        verify(raffleActivityAccountDao).updateAccountQuota(accountCaptor.capture());
        verify(raffleActivityAccountDao, never()).insert(any(RaffleActivityAccountPO.class));
        verify(dbRouter).clear();

        assertEquals("order0000001", orderCaptor.getValue().getOrderId());
        assertEquals("business001", orderCaptor.getValue().getOutBusinessNo());
        assertEquals(Integer.valueOf(10), accountCaptor.getValue().getTotalCountSurplus());
        assertEquals(Integer.valueOf(3), accountCaptor.getValue().getDayCountSurplus());
        assertEquals(Integer.valueOf(5), accountCaptor.getValue().getMonthCountSurplus());
    }

    /** 验证账户不存在时插入订单的同时创建账户。 */
    @Test
    public void doSaveOrder_missingAccount_createsAccount() {
        mockAccountLock();
        CreateQuotaOrderAggregate aggregate = createOrderAggregate();
        when(raffleActivityAccountDao.queryActivityAccountByUserId(any(RaffleActivityAccountPO.class))).thenReturn(null);

        activityRepository.doSaveOrder(aggregate);

        verify(raffleActivityAccountDao).insert(any(RaffleActivityAccountPO.class));
        verify(dbRouter).clear();
    }

    /** 积分支付订单保存成功后必须登记业务号，后续重试才能走数据库幂等查询。 */
    @Test
    public void doSaveCreditPayOrder_success_addsBusinessNumberToBloomFilter() {
        CreateQuotaOrderAggregate aggregate = createOrderAggregate();
        aggregate.getActivityOrderEntity().setState(OrderStateVO.wait_pay);
        aggregate.getActivityOrderEntity().setPayAmount(new BigDecimal("12.50"));

        activityRepository.doSaveCreditPayOrder(aggregate);

        ArgumentCaptor<RaffleActivityOrderPO> captor = ArgumentCaptor.forClass(RaffleActivityOrderPO.class);
        verify(raffleActivityOrderDao).insert(captor.capture());
        assertEquals(OrderStateVO.wait_pay.getCode(), captor.getValue().getState());
        assertEquals(new BigDecimal("12.50"), captor.getValue().getPayAmount());
        verify(orderBloomFilter).add("user001", "business001");
        verify(dbRouter).clear();
    }

    /** 验证布隆过滤器判不存在时跳过数据库查询直接返回 null。 */
    @Test
    public void queryOrder_bloomSaysAbsent_skipsDatabase() {
        when(orderBloomFilter.mightContain("user001", "business001")).thenReturn(false);

        ActivityOrderEntity order = activityRepository.queryActivityOrderByOutBusinessNo("user001", "business001");

        assertNull(order);
        verify(raffleActivityOrderDao, never()).queryByOutBusinessNo("user001", "business001");
        verify(dbRouter, never()).doRouter("user001");
    }

    /** 验证布隆过滤器判可能存在时再向数据库确认订单。 */
    @Test
    public void queryOrder_bloomSaysPossible_confirmsWithDatabase() {
        RaffleActivityOrderPO order = new RaffleActivityOrderPO();
        order.setUserId("user001");
        order.setSku(9011L);
        order.setActivityId(100301L);
        order.setOrderId("123456789012");
        order.setOutBusinessNo("business001");
        order.setPayAmount(new BigDecimal("12.50"));
        order.setState(OrderStateVO.wait_pay.getCode());
        when(orderBloomFilter.mightContain("user001", "business001")).thenReturn(true);
        when(raffleActivityOrderDao.queryByOutBusinessNo("user001", "business001")).thenReturn(order);

        ActivityOrderEntity result = activityRepository.queryActivityOrderByOutBusinessNo("user001", "business001");

        assertEquals("123456789012", result.getOrderId());
        assertEquals("user001", result.getUserId());
        assertEquals(Long.valueOf(9011L), result.getSku());
        assertEquals(Long.valueOf(100301L), result.getActivityId());
        assertEquals("business001", result.getOutBusinessNo());
        assertEquals(new BigDecimal("12.50"), result.getPayAmount());
        assertEquals(OrderStateVO.wait_pay, result.getState());
        verify(dbRouter).doRouter("user001");
        verify(dbRouter).clear();
    }

    /** 验证查询未支付订单时按用户与SKU查询并完整映射返回实体。 */
    @Test
    public void queryUnpaidActivityOrder_mapsOrderFields() {
        RaffleActivityOrderPO order = new RaffleActivityOrderPO();
        order.setUserId("user001");
        order.setSku(9011L);
        order.setOrderId("123456789012");
        order.setOutBusinessNo("business001");
        order.setPayAmount(new BigDecimal("12.50"));
        when(raffleActivityOrderDao.queryUnpaidActivityOrder(any(RaffleActivityOrderPO.class))).thenReturn(order);

        SkuRechargeEntity skuRechargeEntity = SkuRechargeEntity.builder()
                .userId("user001")
                .sku(9011L)
                .outBusinessNo("business001")
                .build();
        UnpaidActivityOrderEntity result = activityRepository.queryUnpaidActivityOrder(skuRechargeEntity);

        assertNotNull(result);
        assertEquals("user001", result.getUserId());
        assertEquals("123456789012", result.getOrderId());
        assertEquals("business001", result.getOutBusinessNo());
        assertEquals(new BigDecimal("12.50"), result.getPayAmount());
    }

    /** 验证不存在未支付订单时返回 null。 */
    @Test
    public void queryUnpaidActivityOrder_missing_returnsNull() {
        when(raffleActivityOrderDao.queryUnpaidActivityOrder(any(RaffleActivityOrderPO.class))).thenReturn(null);

        SkuRechargeEntity skuRechargeEntity = SkuRechargeEntity.builder()
                .userId("user001")
                .sku(9011L)
                .outBusinessNo("business001")
                .build();
        UnpaidActivityOrderEntity result = activityRepository.queryUnpaidActivityOrder(skuRechargeEntity);

        assertNull(result);
    }

    /** 验证重复业务单号触发唯一索引异常时回滚事务并清理路由。 */
    @Test
    public void doSaveOrder_duplicateBusinessNumber_rollsBackAndClearsRoute() {
        mockAccountLock();
        CreateQuotaOrderAggregate aggregate = createOrderAggregate();
        doThrow(new DuplicateKeyException("duplicate out_business_no"))
                .when(raffleActivityOrderDao).insert(any(RaffleActivityOrderPO.class));

        try {
            activityRepository.doSaveOrder(aggregate);
            fail("重复业务单号应抛出唯一索引异常");
        } catch (AppException e) {
            assertEquals(ResponseCode.INDEX_DUP.getCode(), e.getCode());
        }

        verify(transactionStatus).setRollbackOnly();
        verify(raffleActivityAccountDao, never()).updateAccountQuota(any(RaffleActivityAccountPO.class));
        verify(dbRouter).clear();
    }

    /** 验证超时未支付订单批量置为过期时，透传给分表 DAO 执行广播更新。 */
    @Test
    public void updateOrderExpired_delegatesToBroadcastUpdate() {
        Date beforeTime = new Date(1000L);
        when(raffleActivityOrderDao.updateOrderExpired(beforeTime)).thenReturn(3);

        int expiredCount = activityRepository.updateOrderExpired(beforeTime);

        assertEquals(3, expiredCount);
        verify(raffleActivityOrderDao).updateOrderExpired(beforeTime);
        verify(dbRouter, never()).doRouter(anyString());
        verify(dbRouter, never()).clear();
    }

    /** 模拟分布式锁：获取锁成功，避免保存订单时加锁阻塞或超时。 */
    private void mockAccountLock() {
        when(redisService.getLock(anyString())).thenReturn(activityLock);
        try {
            when(activityLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("模拟分布式锁获取被中断", e);
        }
    }

    /** 构造一个创建订单聚合实体的默认测试数据。 */
    private CreateQuotaOrderAggregate createOrderAggregate() {
        ActivityOrderEntity order = ActivityOrderEntity.builder()
                .userId("user001")
                .sku(901100000001L)
                .activityId(100301L)
                .activityName("test activity")
                .strategyId(100006L)
                .orderId("order0000001")
                .orderTime(new Date())
                .totalCount(10)
                .dayCount(3)
                .monthCount(5)
                .state(OrderStateVO.completed)
                .outBusinessNo("business001")
                .build();
        return CreateQuotaOrderAggregate.builder()
                .userId("user001")
                .activityId(100301L)
                .totalCount(10)
                .dayCount(3)
                .monthCount(5)
                .activityOrderEntity(order)
                .build();
    }
}
