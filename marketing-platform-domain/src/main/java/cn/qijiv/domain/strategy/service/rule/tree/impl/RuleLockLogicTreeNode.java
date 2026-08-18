package cn.qijiv.domain.strategy.service.rule.tree.impl;
import cn.qijiv.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.qijiv.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/** 规则树中的次数解锁节点。 */
@Slf4j
@Component("rule_lock")
public class RuleLockLogicTreeNode implements ILogicTreeNode {

    //用户抽奖次数，后续完成这部分流程开发的时候，从数据库/Redis中获取
    private Long userRaffleCount = 10L;

    /**
     * 执行次数解锁判断：用户抽奖次数达到门槛则放行，否则规则接管。
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @param awardId    当前抽中的奖品ID
     * @param ruleValue  次数锁门槛配置值
     * @return 节点执行结果
     */
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId, String ruleValue) {
    log.info("规则过滤-次数锁 userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);
    long raffleCount = 0L;
    try {
        raffleCount = Long.parseLong(ruleValue);
    } catch (Exception e) {
        throw new RuntimeException("规则过滤-次数锁异常 ruleValue: " + ruleValue + " 配置不正确");
    }
    // 用户抽奖次数大于规则限定值，规则放行
    if (userRaffleCount >= raffleCount) {
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.ALLOW)
                .build();
    }
    // 用户抽奖次数小于规则限定值，规则拦截
    return DefaultTreeFactory.TreeActionEntity.builder()
            .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
            .build();
    }

}
