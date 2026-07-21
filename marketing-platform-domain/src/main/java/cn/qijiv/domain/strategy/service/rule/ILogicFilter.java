package cn.qijiv.domain.strategy.service.rule;

import cn.qijiv.domain.strategy.model.entity.RuleActionEntity;
import cn.qijiv.domain.strategy.model.entity.RuleMatterEntity;

/**
 * 抽奖策略规则接口
 *
 * @author jinlujia
 * @date 2026/07/18
 */

public interface ILogicFilter<T extends RuleActionEntity.RaffleEntity> {

    /**
     * 过滤方法
     *
     * @param ruleMatterEntity 抽奖规则物料
     * @return 规则检查结果
     */
    RuleActionEntity<T> filter(RuleMatterEntity ruleMatterEntity);
}