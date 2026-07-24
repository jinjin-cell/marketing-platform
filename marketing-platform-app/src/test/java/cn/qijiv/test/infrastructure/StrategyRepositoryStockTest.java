package cn.qijiv.test.infrastructure;

import cn.qijiv.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.qijiv.infrastructure.persistent.dao.IStrategyAwardDao;
import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import cn.qijiv.infrastructure.persistent.repository.StrategyRespository;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证 Redis 库存扣减和余量防重锁的边界行为。 */
public class StrategyRepositoryStockTest {

    private static final String STOCK_KEY = "big_market_strategy_award_count_key_100001_107";
    private static final String QUEUE_KEY = "strategy_award_count_query_key";

    private StrategyRespository repository;
    private IRedisService redisService;
    private IStrategyAwardDao strategyAwardDao;

    @Before
    public void setUp() {
        repository = new StrategyRespository();
        redisService = mock(IRedisService.class);
        strategyAwardDao = mock(IStrategyAwardDao.class);
        ReflectionTestUtils.setField(repository, "redisService", redisService);
        ReflectionTestUtils.setField(repository, "strategyAwardDao", strategyAwardDao);
    }

    @Test
    public void subtractionAwardStock_positiveSurplusAndUniqueSlot_returnsTrue() {
        when(redisService.decr(STOCK_KEY)).thenReturn(4L);
        when(redisService.setNx(STOCK_KEY + "_4")).thenReturn(true);

        assertTrue(repository.subtractionAwardStock(STOCK_KEY));

        verify(redisService).setNx(STOCK_KEY + "_4");
        verify(redisService, never()).setAtomicLong(STOCK_KEY, 0);
    }

    @Test
    public void subtractionAwardStock_negativeSurplus_restoresZeroAndReturnsFalse() {
        when(redisService.decr(STOCK_KEY)).thenReturn(-1L);

        assertFalse(repository.subtractionAwardStock(STOCK_KEY));

        verify(redisService).setAtomicLong(STOCK_KEY, 0);
        verify(redisService, never()).setNx(STOCK_KEY + "_-1");
    }

    @Test
    public void subtractionAwardStock_duplicateSurplusSlot_returnsFalse() {
        when(redisService.decr(STOCK_KEY)).thenReturn(4L);
        when(redisService.setNx(STOCK_KEY + "_4")).thenReturn(false);

        assertFalse(repository.subtractionAwardStock(STOCK_KEY));
    }

    @Test
    public void cacheStrategyAwardCount_usesAtomicInitialization() {
        repository.cacheStrategyAwardCount(STOCK_KEY, 10);

        verify(redisService).setAtomicLongIfAbsent(STOCK_KEY, 10);
        verify(redisService, never()).setAtomicLong(STOCK_KEY, 10);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void awardStockConsumeSendQueue_validMessage_offersDelayedUpdate() {
        RBlockingQueue<StrategyAwardStockKeyVO> blockingQueue = mock(RBlockingQueue.class);
        RDelayedQueue<StrategyAwardStockKeyVO> delayedQueue = mock(RDelayedQueue.class);
        StrategyAwardStockKeyVO message = StrategyAwardStockKeyVO.builder()
                .strategyId(100001L)
                .awardId(107)
                .build();
        when(redisService.<StrategyAwardStockKeyVO>getBlockingQueue(QUEUE_KEY))
                .thenReturn(blockingQueue);
        when(redisService.getDelayedQueue(blockingQueue)).thenReturn(delayedQueue);

        repository.awardStockConsumeSendQueue(message);

        verify(delayedQueue).offer(message, 3L, TimeUnit.SECONDS);
    }

    @Test
    public void updateStrategyAwardStock_databaseUpdated_invalidatesAwardCache() {
        when(strategyAwardDao.subtractionAwardStock(100001L, 107)).thenReturn(1);

        repository.updateStrategyAwardStock(100001L, 107);

        verify(redisService).delete("big_market_strategy_award_key_v2_100001");
    }
}
