package cn.qijiv.domain.strategy.service.rule.tree.impl;

import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.armory.IStrategyDispatch;
import cn.qijiv.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.qijiv.types.common.Constants;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;


/** 规则树中的兜底奖励节点。 */
@Slf4j
@Component("rule_luck_award")
public class RuleLuckAwardLogicTreeNode implements ILogicTreeNode {

    private final IStrategyDispatch strategyDispatch;
    private final IStrategyRepository strategyRepository;

    public RuleLuckAwardLogicTreeNode(IStrategyDispatch strategyDispatch,
                                      IStrategyRepository strategyRepository) {
        this.strategyDispatch = strategyDispatch;
        this.strategyRepository = strategyRepository;
    }

    /**
     * 执行兜底奖励逻辑：直接返回配置的兜底奖品并接管抽奖。
     *
     * @param userId      用户ID
     * @param strategyId  策略ID
     * @param awardId     当前抽中的奖品ID
     * @param ruleValue   兜底奖品配置，格式为 奖品ID[:奖品规则配置]
     * @param endDateTime 活动结束时间，用于设置兜底奖品库存锁的有效期
     * @return 节点执行结果
     */
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId, String ruleValue, Date endDateTime) {
    log.info("规则过滤-兜底奖品 userId:{} strategyId:{} awardId:{} ruleValue:{}", userId, strategyId, awardId, ruleValue);
    String[] split = ruleValue.split(Constants.COLON);
    if (split.length == 0) {
        log.error("规则过滤-兜底奖品，兜底奖品未配置告警 userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);
        throw new RuntimeException("兜底奖品未配置 " + ruleValue);
    }
    // 兜底奖励配置
    Integer luckAwardId = Integer.valueOf(split[0]);
    String awardRuleValue = split.length > 1 ? split[1] : "";
    // 次数锁和库存不足分支会直接跳到本节点，因此兜底奖品必须在这里独立扣减库存。
    Boolean stockAvailable = strategyDispatch.subtractionAwardStock(
            strategyId, luckAwardId, endDateTime);
    if (!Boolean.TRUE.equals(stockAvailable)) {
        log.warn("规则过滤-兜底奖品库存不足 userId:{} strategyId:{} awardId:{}",
                userId, strategyId, luckAwardId);
        throw new AppException(ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getCode(),
                ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getInfo());
    }
    strategyRepository.awardStockConsumeSendQueue(StrategyAwardStockKeyVO.builder()
            .strategyId(strategyId)
            .awardId(luckAwardId)
            .build());

    // 返回已完成库存预占的兜底奖品。
    log.info("规则过滤-兜底奖品 userId:{} strategyId:{} awardId:{} awardRuleValue:{}", userId, strategyId, luckAwardId, awardRuleValue);
    return DefaultTreeFactory.TreeActionEntity.builder()
            .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
            .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                    .awardId(luckAwardId)
                    .awardRuleValue(awardRuleValue)
                    .build())
            .build();
    }

}
