package cn.qijiv.domain.strategy.service.rule.chain.impl;

import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyRuleEntity;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.qijiv.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.qijiv.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/** 黑名单抽奖责任链节点。 */
@Slf4j
@Component("rule_blacklist")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BackListLogicChain extends AbstractLogicChain {

    /** 领域仓储，用于查询黑名单规则配置。 */
    private final IStrategyRepository repository;

    /**
     * 注入领域仓储。
     *
     * @param repository 领域仓储
     */
    public BackListLogicChain(IStrategyRepository repository) {
        this.repository = repository;
    }

    /**
     * 执行黑名单规则判断：用户命中黑名单时直接接管抽奖，否则放行到下一个节点。
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @return 责任链抽奖结果，黑名单命中时为固定奖品
     */
    @Override
    public DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        // 黑名单是策略级前置规则，配置格式为：固定奖品ID:用户ID,用户ID,...
        StrategyRuleEntity rule = repository.queryStrategyRule(strategyId, ruleModel());
        if (rule == null || StringUtils.isBlank(rule.getRuleValue())) {
            throw new IllegalArgumentException("黑名单规则配置不存在");
        }

        // 先拆出固定奖品 ID 和黑名单用户列表；limit=2 避免后续内容被错误拆散。
        String[] ruleValueParts = rule.getRuleValue().split(Constants.COLON, 2);
        if (ruleValueParts.length != 2
                || StringUtils.isBlank(ruleValueParts[0])
                || StringUtils.isBlank(ruleValueParts[1])) {
            throw new IllegalArgumentException("rule_blacklist invalid input format: " + rule.getRuleValue());
        }

        final Integer awardId;
        try {
            awardId = Integer.parseInt(ruleValueParts[0].trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("rule_blacklist invalid awardId: " + ruleValueParts[0], ex);
        }

        // 逐个比较用户 ID。命中后直接返回固定奖品，责任链在此短路，不再执行后续节点。
        for (String blackUserId : ruleValueParts[1].split(Constants.SPLIT)) {
            if (userId.equals(blackUserId.trim())) {
                validateAward(strategyId, awardId);
                log.info("抽奖责任链-黑名单接管 userId:{} strategyId:{} awardId:{}",
                        userId, strategyId, awardId);
                return DefaultChainFactory.StrategyAwardVO.builder()
                        .awardId(awardId)
                        .logicModel(ruleModel())
                        .build();
            }
        }

        // 未命中黑名单，放行到权重规则或默认抽奖节点。
        return nextLogic(userId, strategyId);
    }

    /**
     * 校验黑名单规则指定的奖品是否属于当前策略。
     *
     * @param strategyId 策略ID
     * @param awardId    奖品ID
     */
    private void validateAward(Long strategyId, Integer awardId) {
        List<StrategyAwardEntity> strategyAwards = repository.queryStrategyAwardList(strategyId);
        if (strategyAwards == null || strategyAwards.stream()
                .noneMatch(award -> award != null && awardId.equals(award.getAwardId()))) {
            throw new IllegalArgumentException(
                    "黑名单规则指定的奖品不属于当前策略，strategyId: " + strategyId + ", awardId: " + awardId);
        }
    }

    /**
     * 返回本节点的规则模型名称。
     *
     * @return 规则模型名称，即 {@code rule_blacklist}
     */
    @Override
    protected String ruleModel() {
        return DefaultChainFactory.RULE_BLACKLIST;
    }
}
