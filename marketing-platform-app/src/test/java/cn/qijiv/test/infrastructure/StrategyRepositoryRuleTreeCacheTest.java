package cn.qijiv.test.infrastructure;

import cn.qijiv.domain.strategy.model.valobj.RuleTreeVO;
import cn.qijiv.infrastructure.persistent.dao.IRuleTreeDao;
import cn.qijiv.infrastructure.persistent.dao.IRuleTreeNodeDao;
import cn.qijiv.infrastructure.persistent.dao.IRuleTreeNodeLineDao;
import cn.qijiv.infrastructure.persistent.po.RuleTreeNodePO;
import cn.qijiv.infrastructure.persistent.po.RuleTreePO;
import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import cn.qijiv.infrastructure.persistent.repository.StrategyRespository;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证规则树查询遵循“先查缓存，未命中再查数据库”的流程。 */
public class StrategyRepositoryRuleTreeCacheTest {

    private static final String TREE_ID = "tree_lock";
    private static final String CACHE_KEY = "big_market_rule_tree_key_v1_" + TREE_ID;

    private StrategyRespository repository;
    private IRedisService redisService;
    private IRuleTreeDao ruleTreeDao;
    private IRuleTreeNodeDao ruleTreeNodeDao;
    private IRuleTreeNodeLineDao ruleTreeNodeLineDao;

    @Before
    public void setUp() {
        repository = new StrategyRespository();
        redisService = mock(IRedisService.class);
        ruleTreeDao = mock(IRuleTreeDao.class);
        ruleTreeNodeDao = mock(IRuleTreeNodeDao.class);
        ruleTreeNodeLineDao = mock(IRuleTreeNodeLineDao.class);

        // 仓储使用 @Resource 注入依赖，单元测试通过反射注入模拟对象，避免连接真实 Redis 和 MySQL。
        ReflectionTestUtils.setField(repository, "redisService", redisService);
        ReflectionTestUtils.setField(repository, "ruleTreeDao", ruleTreeDao);
        ReflectionTestUtils.setField(repository, "ruleTreeNodeDao", ruleTreeNodeDao);
        ReflectionTestUtils.setField(repository, "ruleTreeNodeLineDao", ruleTreeNodeLineDao);
    }

    @Test
    public void test_queryRuleTreeVOByTreeId_cacheMissThenCacheHit() {
        RuleTreePO treePO = new RuleTreePO();
        treePO.setTreeId(TREE_ID);
        treePO.setTreeName("抽奖解锁规则树");
        treePO.setTreeDesc("测试规则树缓存");
        treePO.setTreeNodeRuleKey("rule_lock");

        RuleTreeNodePO nodePO = new RuleTreeNodePO();
        nodePO.setTreeId(TREE_ID);
        nodePO.setRuleKey("rule_lock");
        nodePO.setRuleDesc("抽奖次数解锁");
        nodePO.setRuleValue("1");

        // 第一次 Redis 返回 null，仓储需要从三张规则树表组装数据。
        when(redisService.getValue(CACHE_KEY)).thenReturn(null);
        when(ruleTreeDao.queryRuleTreeByTreeId(TREE_ID)).thenReturn(treePO);
        when(ruleTreeNodeDao.queryRuleTreeNodeListByTreeId(TREE_ID))
                .thenReturn(Collections.singletonList(nodePO));
        when(ruleTreeNodeLineDao.queryRuleTreeNodeLineListByTreeId(TREE_ID))
                .thenReturn(Collections.emptyList());

        RuleTreeVO databaseResult = repository.queryRuleTreeVOByTreeId(TREE_ID);

        verify(redisService).setValue(CACHE_KEY, databaseResult, 30L, TimeUnit.MINUTES);
        verify(ruleTreeDao).queryRuleTreeByTreeId(TREE_ID);
        verify(ruleTreeNodeDao).queryRuleTreeNodeListByTreeId(TREE_ID);
        verify(ruleTreeNodeLineDao).queryRuleTreeNodeLineListByTreeId(TREE_ID);

        // 模拟第一次查询已经写入 Redis；第二次应直接返回缓存对象，不再访问数据库。
        when(redisService.getValue(CACHE_KEY)).thenReturn(databaseResult);
        RuleTreeVO cacheResult = repository.queryRuleTreeVOByTreeId(TREE_ID);

        assertSame(databaseResult, cacheResult);
        verify(ruleTreeDao).queryRuleTreeByTreeId(TREE_ID);
        verify(ruleTreeNodeDao).queryRuleTreeNodeListByTreeId(TREE_ID);
        verify(ruleTreeNodeLineDao).queryRuleTreeNodeLineListByTreeId(TREE_ID);
    }

    @Test
    public void test_storeStrategyRateTable_setsExpiration() {
        repository.storeStrategyRateTable("100001", Collections.singletonList(101));

        verify(redisService).setList(
                "big_market_strategy_rate_table_key_100001",
                Collections.singletonList(101),
                30L,
                TimeUnit.MINUTES);
    }
}
