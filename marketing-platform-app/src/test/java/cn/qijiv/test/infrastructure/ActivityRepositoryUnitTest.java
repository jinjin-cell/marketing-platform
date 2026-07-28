package cn.qijiv.test.infrastructure;

import cn.qijiv.domain.activity.model.entity.ActivityCountEntity;
import cn.qijiv.domain.activity.model.entity.ActivityEntity;
import cn.qijiv.domain.activity.model.entity.ActivitySkuEntity;
import cn.qijiv.domain.activity.model.valobj.ActivityStateVO;
import cn.qijiv.infrastructure.persistent.dao.IRaffleActivityCountDao;
import cn.qijiv.infrastructure.persistent.dao.IRaffleActivityDao;
import cn.qijiv.infrastructure.persistent.dao.IRaffleActivitySkuDao;
import cn.qijiv.infrastructure.persistent.po.RaffleActivityCountPO;
import cn.qijiv.infrastructure.persistent.po.RaffleActivityPO;
import cn.qijiv.infrastructure.persistent.po.RaffleActivitySkuPO;
import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import cn.qijiv.infrastructure.persistent.repository.ActivityRepository;
import cn.qijiv.types.common.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
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

    @InjectMocks
    private ActivityRepository activityRepository;

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
}
