package cn.qijiv.test.infrastructure;

import cn.qijiv.domain.activity.model.aggregate.CreateOrderAggregate;
import cn.qijiv.domain.activity.model.entity.ActivityCountEntity;
import cn.qijiv.domain.activity.model.entity.ActivityEntity;
import cn.qijiv.domain.activity.model.entity.ActivityOrderEntity;
import cn.qijiv.domain.activity.model.entity.ActivitySkuEntity;
import cn.qijiv.domain.activity.model.valobj.ActivityStateVO;
import cn.qijiv.domain.activity.model.valobj.OrderStateVO;
import cn.qijiv.infrastructure.persistent.dao.IRaffleActivityAccountDao;
import cn.qijiv.infrastructure.persistent.dao.IRaffleActivityCountDao;
import cn.qijiv.infrastructure.persistent.dao.IRaffleActivityDao;
import cn.qijiv.infrastructure.persistent.dao.IRaffleActivityOrderDao;
import cn.qijiv.infrastructure.persistent.dao.IRaffleActivitySkuDao;
import cn.qijiv.infrastructure.persistent.db.IDBRouterStrategy;
import cn.qijiv.infrastructure.persistent.po.RaffleActivityAccountPO;
import cn.qijiv.infrastructure.persistent.po.RaffleActivityCountPO;
import cn.qijiv.infrastructure.persistent.po.RaffleActivityOrderPO;
import cn.qijiv.infrastructure.persistent.po.RaffleActivityPO;
import cn.qijiv.infrastructure.persistent.po.RaffleActivitySkuPO;
import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import cn.qijiv.infrastructure.persistent.redis.OrderBusinessNoBloomFilter;
import cn.qijiv.infrastructure.persistent.repository.ActivityRepository;
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

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActivityRepositoryUnitTest {

    @Mock
    private IRedisService redisService;
    @Mock
    private IRaffleActivityDao raffleActivityDao;
    @Mock
    private IRaffleActivitySkuDao raffleActivitySkuDao;
    @Mock
    private IRaffleActivityCountDao raffleActivityCountDao;
    @Mock
    private IRaffleActivityOrderDao raffleActivityOrderDao;
    @Mock
    private IRaffleActivityAccountDao raffleActivityAccountDao;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private TransactionStatus transactionStatus;
    @Mock
    private IDBRouterStrategy dbRouter;
    @Mock
    private OrderBusinessNoBloomFilter orderBloomFilter;

    @InjectMocks
    private ActivityRepository activityRepository;

    @Before
    public void executeTransactionCallbacks() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
    }

    @Test
    public void queryActivitySku_existingSku_mapsAllFields() {
        RaffleActivitySkuPO skuPO = new RaffleActivitySkuPO();
        skuPO.setSku(10001L);
        skuPO.setActivityId(20001L);
        skuPO.setActivityCountId(30001L);
        skuPO.setStockCount(100);
        skuPO.setStockCountSurplus(40);
        when(raffleActivitySkuDao.queryRaffleActivitySkuBySku(10001L)).thenReturn(skuPO);

        ActivitySkuEntity result = activityRepository.queryActivitySku(10001L);

        assertEquals(Long.valueOf(10001L), result.getSku());
        assertEquals(Long.valueOf(20001L), result.getActivityId());
        assertEquals(Long.valueOf(30001L), result.getActivityCountId());
        assertEquals(Integer.valueOf(100), result.getStockCount());
        assertEquals(Integer.valueOf(40), result.getStockCountSurplus());
        verify(raffleActivitySkuDao).queryRaffleActivitySkuBySku(10001L);
    }

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

    @Test
    public void doSaveOrder_existingAccount_insertsOrderAndAddsQuota() {
        CreateOrderAggregate aggregate = createOrderAggregate();
        when(raffleActivityAccountDao.updateAccountQuota(any(RaffleActivityAccountPO.class))).thenReturn(1);

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

    @Test
    public void doSaveOrder_missingAccount_createsAccount() {
        CreateOrderAggregate aggregate = createOrderAggregate();
        when(raffleActivityAccountDao.updateAccountQuota(any(RaffleActivityAccountPO.class))).thenReturn(0);

        activityRepository.doSaveOrder(aggregate);

        verify(raffleActivityAccountDao).insert(any(RaffleActivityAccountPO.class));
        verify(dbRouter).clear();
    }

    @Test
    public void queryOrder_bloomSaysAbsent_skipsDatabase() {
        when(orderBloomFilter.mightContain("user001", "business001")).thenReturn(false);

        String orderId = activityRepository.queryOrderIdByOutBusinessNo("user001", "business001");

        assertEquals(null, orderId);
        verify(raffleActivityOrderDao, never()).queryByOutBusinessNo("user001", "business001");
        verify(dbRouter, never()).doRouter("user001");
    }

    @Test
    public void queryOrder_bloomSaysPossible_confirmsWithDatabase() {
        RaffleActivityOrderPO order = new RaffleActivityOrderPO();
        order.setOrderId("123456789012");
        when(orderBloomFilter.mightContain("user001", "business001")).thenReturn(true);
        when(raffleActivityOrderDao.queryByOutBusinessNo("user001", "business001")).thenReturn(order);

        String orderId = activityRepository.queryOrderIdByOutBusinessNo("user001", "business001");

        assertEquals("123456789012", orderId);
        verify(dbRouter).doRouter("user001");
        verify(dbRouter).clear();
    }

    @Test
    public void doSaveOrder_duplicateBusinessNumber_rollsBackAndClearsRoute() {
        CreateOrderAggregate aggregate = createOrderAggregate();
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

    private CreateOrderAggregate createOrderAggregate() {
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
        return CreateOrderAggregate.builder()
                .userId("user001")
                .activityId(100301L)
                .totalCount(10)
                .dayCount(3)
                .monthCount(5)
                .activityOrderEntity(order)
                .build();
    }
}
