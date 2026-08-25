package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.persistent.dao.IRuleTreeNodeDao;
import cn.qijiv.infrastructure.persistent.po.RuleTreeNodePO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 规则树节点 DAO 单元测试
 *
 * <p>验证批量查询规则树中的 rule_lock 次数解锁节点配置，
 * 该配置用于奖品列表接口返回「抽奖N次后解锁」信息。</p>
 *
 * @author qijiv
 * @since 2026-08-19
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RuleTreeNodeDaoTest {

    /** 规则树节点 DAO。 */
    @Resource
    private IRuleTreeNodeDao ruleTreeNodeDao;

    /** 验证批量查询次数解锁节点并打印结果。 */
    @Test
    public void test_queryRuleLockNodeListByTreeIds() {
        List<RuleTreeNodePO> nodes =
                ruleTreeNodeDao.queryRuleLockNodeListByTreeIds(Arrays.asList("tree_lock_1", "tree_lock_2"));
        log.info("测试结果：{}", JSON.toJSONString(nodes));
    }

}
