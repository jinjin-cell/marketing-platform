package cn.qijiv.domain.strategy.service.raffle;

import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.IRaffleRule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 抽奖规则查询服务实现
 *
 * @author qijiv
 * @since 2026-07-18
 */
@Service
public class RaffleRule implements IRaffleRule {

    /** 策略仓储 */
    private final IStrategyRepository repository;

    public RaffleRule(IStrategyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Map<String, Integer> queryAwardRuleLockCount(List<String> treeIds) {
        return repository.queryAwardRuleLockCount(treeIds);
    }
}
