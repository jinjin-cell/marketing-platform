package cn.qijiv.domain.strategy.service.rule.chain.factory;

import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.rule.chain.ILogicChain;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 按策略配置装配抽奖前置责任链。 */
@Service
public class DefaultChainFactory {

    /** 默认概率抽奖节点的 Spring Bean 名称，同时也是数据库规则模型名称。 */
    public static final String DEFAULT_CHAIN = "default";
    /** 黑名单责任链节点的 Spring Bean 名称，同时也是数据库规则模型名称。 */
    public static final String RULE_BLACKLIST = "rule_blacklist";
    /** 权重责任链节点的 Spring Bean 名称，同时也是数据库规则模型名称。 */
    public static final String RULE_WEIGHT = "rule_weight";

    /** Spring Bean 容器，用于按名称获取责任链节点实例。 */
    private final ListableBeanFactory beanFactory;
    /** 领域仓储，用于查询策略及其规则配置。 */
    private final IStrategyRepository repository;

    /**
     * 注入 Spring Bean 容器和领域仓储。
     *
     * @param beanFactory Spring Bean 容器
     * @param repository  领域仓储
     */
    public DefaultChainFactory(ListableBeanFactory beanFactory, IStrategyRepository repository) {
        this.beanFactory = beanFactory;
        this.repository = repository;
    }

    /**
     * 按策略配置打开一条抽奖前置责任链，链尾始终追加默认概率抽奖节点。
     *
     * @param strategyId 策略ID
     * @return 责任链头节点
     */
    public ILogicChain openLogicChain(Long strategyId) {
        StrategyEntity strategy = repository.queryStrategyEntityByStrategyId(strategyId);
        if (strategy == null) {
            throw new IllegalArgumentException("抽奖策略不存在，strategyId: " + strategyId);
        }

        List<String> ruleModels = orderedRuleModels(strategy.ruleModels());
        ILogicChain head = null;
        ILogicChain current = null;
        for (String ruleModel : ruleModels) {
            // 每次从 Spring 获取一个新的 prototype 节点，避免并发请求共享 next 配置。
            ILogicChain next = getChain(ruleModel);
            if (head == null) {
                head = next;
                current = next;
            } else {
                // appendNext 返回新节点，current 随装配过程向链尾移动。
                current = current.appendNext(next);
            }
        }

        // 默认节点始终是责任链的最后一环，保证前置规则都未接管时仍能正常概率抽奖。
        ILogicChain defaultChain = getChain(DEFAULT_CHAIN);
        if (head == null) {
            return defaultChain;
        }
        current.appendNext(defaultChain);
        return head;
    }

    /**
     * 按配置顺序装配抽奖责任链节点。
     *
     * <p>黑名单规则必须优先于权重规则执行，否则会覆盖权重规则。</p>
     */
    private List<String> orderedRuleModels(String[] configuredRuleModels) {
        // LinkedHashSet 同时完成去重和保留数据库中的配置顺序。
        Set<String> uniqueRuleModels = new LinkedHashSet<>();
        if (configuredRuleModels != null) {
            for (String configuredRuleModel : configuredRuleModels) {
                String ruleModel = StringUtils.trimToEmpty(configuredRuleModel);
                if (!ruleModel.isEmpty()) {
                    uniqueRuleModels.add(ruleModel);
                }
            }
        }

        List<String> ordered = new ArrayList<>();
        if (uniqueRuleModels.remove(RULE_BLACKLIST)) {
            // 黑名单命中后会直接指定奖品，因此必须优先于权重规则执行。
            ordered.add(RULE_BLACKLIST);
        }
        ordered.addAll(uniqueRuleModels);
        return ordered;
    }

    /**
     * 按 Spring Bean 名称获取责任链节点实例。
     *
     * @param beanName 责任链节点 Bean 名称
     * @return 责任链节点实例
     */
    private ILogicChain getChain(String beanName) {
        try {
            return beanFactory.getBean(beanName, ILogicChain.class);
        } catch (NoSuchBeanDefinitionException ex) {
            throw new IllegalStateException("抽奖责任链节点未注册，ruleModel: " + beanName, ex);
        }
    }

    /**
     * 责任链抽奖结果。
     *
     * <p>除了奖品ID，还必须记录最终由哪个节点返回结果。模板方法会使用
     * {@code logicModel} 判断：黑名单、权重等规则已经接管时直接返回；只有
     * {@code default} 普通概率抽奖才继续进入规则树做次数和库存校验。</p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategyAwardVO {

        /** 责任链最终选出的奖品ID。 */
        private Integer awardId;
        /** 最终返回奖品的责任链节点名称。 */
        private String logicModel;
    }
}
