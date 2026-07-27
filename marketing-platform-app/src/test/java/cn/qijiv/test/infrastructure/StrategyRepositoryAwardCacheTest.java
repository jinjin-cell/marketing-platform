package cn.qijiv.test.infrastructure;

import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.infrastructure.persistent.dao.IStrategyAwardDao;
import cn.qijiv.infrastructure.persistent.po.StrategyAwardPO;
import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import cn.qijiv.infrastructure.persistent.repository.StrategyRespository;
import cn.qijiv.types.common.Constants;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证单奖品查询复用策略奖品列表缓存。 */
public class StrategyRepositoryAwardCacheTest {

    private static final Long STRATEGY_ID = 100001L;
    private static final String CACHE_KEY =
            Constants.RedisKey.STRATEGY_AWARD_LIST_KEY + STRATEGY_ID;

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
    public void queryStrategyAwardEntity_cacheHit_reusesAwardList() {
        StrategyAwardEntity first = StrategyAwardEntity.builder().awardId(101).build();
        StrategyAwardEntity expected = StrategyAwardEntity.builder().awardId(102).sort(2).build();
        when(redisService.<List<StrategyAwardEntity>>getValue(CACHE_KEY))
                .thenReturn(Arrays.asList(first, expected));

        StrategyAwardEntity result = repository.queryStrategyAwardEntity(STRATEGY_ID, 102);

        assertSame(expected, result);
        verify(strategyAwardDao, never()).queryStrategyAwardListByStrategyId(STRATEGY_ID);
    }

    @Test
    public void queryStrategyAwardEntity_cacheMiss_loadsAndCachesAwardList() {
        StrategyAwardPO awardPO = new StrategyAwardPO();
        awardPO.setStrategyId(STRATEGY_ID);
        awardPO.setAwardId(103);
        awardPO.setAwardTitle("模型使用次数");
        awardPO.setAwardSubtitle("增加10次");
        awardPO.setSort(3);
        when(redisService.<List<StrategyAwardEntity>>getValue(CACHE_KEY)).thenReturn(null);
        when(strategyAwardDao.queryStrategyAwardListByStrategyId(STRATEGY_ID))
                .thenReturn(Collections.singletonList(awardPO));

        StrategyAwardEntity result = repository.queryStrategyAwardEntity(STRATEGY_ID, 103);

        assertEquals(Integer.valueOf(103), result.getAwardId());
        assertEquals("模型使用次数", result.getAwardTitle());
        assertEquals(Integer.valueOf(3), result.getSort());
        verify(redisService).setValue(
                CACHE_KEY,
                Collections.singletonList(result),
                10L,
                TimeUnit.MINUTES);
    }

    @Test
    public void queryStrategyAwardEntity_awardNotFound_returnsNull() {
        StrategyAwardEntity award = StrategyAwardEntity.builder().awardId(101).build();
        when(redisService.<List<StrategyAwardEntity>>getValue(CACHE_KEY))
                .thenReturn(Collections.singletonList(award));

        assertNull(repository.queryStrategyAwardEntity(STRATEGY_ID, 999));
    }
}
