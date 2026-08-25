package cn.qijiv.domain.strategy.service;

import java.util.List;
import java.util.Map;

/**
 * 抽奖规则查询服务
 *
 * @author qijiv
 * @since 2026-07-18
 */
public interface IRaffleRule {

    /**
     * 批量查询奖品规则树的解锁次数
     *
     * <p>根据规则树ID列表，查询每棵规则树中 rule_lock 节点配置的解锁次数，
     * 返回规则树ID到解锁次数的映射。未配置 rule_lock 节点的规则树不会出现在结果中。</p>
     *
     * @param treeIds 规则树ID列表
     * @return 规则树ID -> 解锁次数
     */
    Map<String, Integer> queryAwardRuleLockCount(List<String> treeIds);
}
