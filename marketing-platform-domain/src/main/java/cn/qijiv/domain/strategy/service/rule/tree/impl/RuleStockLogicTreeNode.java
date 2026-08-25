package cn.qijiv.domain.strategy.service.rule.tree.impl;

import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.armory.IStrategyDispatch;
import cn.qijiv.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/** 规则树中的库存校验和扣减节点。 */
@Slf4j
@Component("rule_stock")
public class RuleStockLogicTreeNode implements ILogicTreeNode {

    /** 策略抽奖调度服务，负责扣减奖品库存。 */
    private final IStrategyDispatch strategyDispatch;

    /** 领域仓储，负责将库存扣减消息发送到延迟队列。 */
    private final IStrategyRepository strategyRepository;

    /**
     * 注入策略抽奖调度服务和领域仓储。
     *
     * @param strategyDispatch   策略抽奖调度服务
     * @param strategyRepository 领域仓储
     */
    public RuleStockLogicTreeNode(IStrategyDispatch strategyDispatch, IStrategyRepository strategyRepository) {
        this.strategyDispatch = strategyDispatch;
        this.strategyRepository = strategyRepository;
    }

    /**
     * 执行库存扣减校验：扣减成功则接管并返回奖品，库存不足则放行走兜底分支。
     *
     * @param userId      用户ID
     * @param strategyId  策略ID
     * @param awardId     当前抽中的奖品ID
     * @param ruleValue   节点配置值
     * @param endDateTime 活动结束时间，用于设置库存锁缓存的有效期
     * @return 节点执行结果
     */
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId, String ruleValue, Date endDateTime) {
        log.info("规则过滤-库存扣减 userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);
        // 扣减库存
        Boolean status = strategyDispatch.subtractionAwardStock(strategyId, awardId, endDateTime);
        // true；库存扣减成功，TAKE_OVER 规则节点接管，返回奖品ID，奖品规则配置
        if (Boolean.TRUE.equals(status)) {
            log.info("规则过滤-库存扣减-成功 userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);
            // 写入延迟队列，延迟消费更新数据库记录。【在trigger的job；UpdateAwardStockJob 下消费队列，更新数据库记录】
            strategyRepository.awardStockConsumeSendQueue(StrategyAwardStockKeyVO.builder()
                    .strategyId(strategyId)
                    .awardId(awardId)
                    .build());
            return DefaultTreeFactory.TreeActionEntity.builder()
                    .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                    .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                            .awardId(awardId)
                            .awardRuleValue(ruleValue)
                            .build())
                    .build();

        }
        // 如果库存不足，则直接返回放行，由规则树继续走兜底分支。
        log.warn("规则过滤-库存扣减-告警，库存不足。userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.ALLOW)
                .build();
    }
}
