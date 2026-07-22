package cn.qijiv.domain.strategy.service.rule.chain.factory;

import cn.qijiv.domain.strategy.model.entity.StrategyEntity;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.rule.chain.ILogicChain;
import cn.qijiv.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 按策略配置装配抽奖前置责任链。 */
@Service
public class DefaultChainFactory {

    private static final String DEFAULT_CHAIN = "default";

    private final ListableBeanFactory beanFactory;
    private final IStrategyRepository repository;

    public DefaultChainFactory(ListableBeanFactory beanFactory, IStrategyRepository repository) {
        this.beanFactory = beanFactory;
        this.repository = repository;
    }

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

        String blacklist = DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode();
        List<String> ordered = new ArrayList<>();
        if (uniqueRuleModels.remove(blacklist)) {
            // 黑名单命中后会直接指定奖品，因此必须优先于权重规则执行。
            ordered.add(blacklist);
        }
        ordered.addAll(uniqueRuleModels);
        return ordered;
    }

    private ILogicChain getChain(String beanName) {
        try {
            return beanFactory.getBean(beanName, ILogicChain.class);
        } catch (NoSuchBeanDefinitionException ex) {
            throw new IllegalStateException("抽奖责任链节点未注册，ruleModel: " + beanName, ex);
        }
    }
}
