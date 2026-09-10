package cn.qijiv.test.infrastructure;

import cn.qijiv.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.qijiv.infrastructure.dao.IStrategyAwardDao;
import cn.qijiv.infrastructure.redis.IRedisService;
import cn.qijiv.infrastructure.adapter.repository.StrategyRespository;
import cn.qijiv.types.common.Constants;
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

    /** 初始化仓储，并通过反射注入 Mock 的 Redis 与 DAO 依赖。 */
    @Before
    public void setUp() {
        repository = new StrategyRespository();
        redisService = mock(IRedisService.class);
        strategyAwardDao = mock(IStrategyAwardDao.class);
        ReflectionTestUtils.setField(repository, "redisService", redisService);
        ReflectionTestUtils.setField(repository, "strategyAwardDao", strategyAwardDao);
    }

    /** 库存余量大于 0 且槽位唯一时扣减成功。 */
    @Test
    public void subtractionAwardStock_positiveSurplusAndUniqueSlot_returnsTrue() {
        when(redisService.decr(STOCK_KEY)).thenReturn(4L);
        when(redisService.setNx(STOCK_KEY + "_4", TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS))
                .thenReturn(true);

        assertTrue(repository.subtractionAwardStock(STOCK_KEY));

        verify(redisService).setNx(STOCK_KEY + "_4", TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
        verify(redisService, never()).setAtomicLong(STOCK_KEY, 0);
    }

    /** 库存余量为负时回写 0 并返回扣减失败。 */
    @Test
    public void subtractionAwardStock_negativeSurplus_restoresZeroAndReturnsFalse() {
        when(redisService.decr(STOCK_KEY)).thenReturn(-1L);

        assertFalse(repository.subtractionAwardStock(STOCK_KEY));

        verify(redisService).setAtomicLong(STOCK_KEY, 0);
        verify(redisService, never()).setNx(STOCK_KEY + "_-1");
    }

    /** 库存余量槽位重复（并发冲突）时扣减失败。 */
    @Test
    public void subtractionAwardStock_duplicateSurplusSlot_returnsFalse() {
        when(redisService.decr(STOCK_KEY)).thenReturn(4L);
        when(redisService.setNx(STOCK_KEY + "_4", TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS))
                .thenReturn(false);

        assertFalse(repository.subtractionAwardStock(STOCK_KEY));
    }

    /** 验证奖品库存缓存使用原子初始化，不会重复覆盖已存在值。 */
    @Test
    public void cacheStrategyAwardCount_usesAtomicInitialization() {
        repository.cacheStrategyAwardCount(STOCK_KEY, 10);

        verify(redisService).setAtomicLongIfAbsent(STOCK_KEY, 10);
        verify(redisService, never()).setAtomicLong(STOCK_KEY, 10);
    }

    /** 验证库存扣减消息成功写入延迟队列。 */
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

    /** 数据库库存更新成功后删除奖品列表缓存。 */
    @Test
    public void updateStrategyAwardStock_databaseUpdated_invalidatesAwardCache() {
        when(strategyAwardDao.subtractionAwardStock(100001L, 107)).thenReturn(1);

        repository.updateStrategyAwardStock(100001L, 107);

        verify(redisService).delete(Constants.RedisKey.STRATEGY_AWARD_LIST_KEY + "100001");
    }
}
