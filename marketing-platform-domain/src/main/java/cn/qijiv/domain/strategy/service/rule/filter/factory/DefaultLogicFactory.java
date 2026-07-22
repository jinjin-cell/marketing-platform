package cn.qijiv.domain.strategy.service.rule.filter.factory;

import cn.qijiv.domain.strategy.model.entity.RuleActionEntity;
import cn.qijiv.domain.strategy.service.annotation.LogicStrategy;
import cn.qijiv.domain.strategy.service.rule.ILogicFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 规则过滤器工厂。 */
@Service
public class DefaultLogicFactory {

    private final Map<String, ILogicFilter<RuleActionEntity.RaffleCenterEntity>> centerLogicFilterMap;
    private final Map<String, ILogicFilter<RuleActionEntity.RaffleEntity>> afterLogicFilterMap;

    @SuppressWarnings("unchecked")
    public DefaultLogicFactory(List<? extends ILogicFilter<?>> logicFilters) {
        Map<String, ILogicFilter<RuleActionEntity.RaffleCenterEntity>> centerFilters = new LinkedHashMap<>();
        Map<String, ILogicFilter<RuleActionEntity.RaffleEntity>> afterFilters = new LinkedHashMap<>();
        for (ILogicFilter<?> logicFilter : logicFilters) {
            // Spring 注入全部过滤器后，通过注解建立“规则模型 -> 过滤器”的路由表。
            LogicStrategy strategy = AnnotationUtils.findAnnotation(logicFilter.getClass(), LogicStrategy.class);
            if (strategy == null) {
                continue;
            }
            String ruleModel = strategy.logicMode().getCode();
            String ruleType = strategy.logicMode().getType();
            if (LogicType.CENTER.getCode().equals(ruleType)) {
                registerFilter(centerFilters, ruleModel,
                        (ILogicFilter<RuleActionEntity.RaffleCenterEntity>) logicFilter);
            } else if (LogicType.AFTER.getCode().equals(ruleType)) {
                registerFilter(afterFilters, ruleModel,
                        (ILogicFilter<RuleActionEntity.RaffleEntity>) logicFilter);
            } else if (LogicType.BEFORE.getCode().equals(ruleType)) {
                throw new IllegalStateException("抽奖前置规则必须使用责任链，ruleModel: " + ruleModel);
            } else {
                throw new IllegalStateException("不支持的抽奖规则阶段，ruleModel: "
                        + ruleModel + ", type: " + ruleType);
            }
        }
        this.centerLogicFilterMap = Collections.unmodifiableMap(centerFilters);
        this.afterLogicFilterMap = Collections.unmodifiableMap(afterFilters);
    }

    private <T extends RuleActionEntity.RaffleEntity> void registerFilter(
            Map<String, ILogicFilter<T>> filters,
            String ruleModel,
            ILogicFilter<T> logicFilter) {
        if (filters.put(ruleModel, logicFilter) != null) {
            throw new IllegalStateException("重复的抽奖规则过滤器，ruleModel: " + ruleModel);
        }
    }

    public Map<String, ILogicFilter<RuleActionEntity.RaffleCenterEntity>> openCenterLogicFilter() {
        return centerLogicFilterMap;
    }

    /** 后置规则流程接入后使用。 */
    @SuppressWarnings("unused")
    public Map<String, ILogicFilter<RuleActionEntity.RaffleEntity>> openAfterLogicFilter() {
        return afterLogicFilterMap;
    }


    @Getter
    @AllArgsConstructor
    public enum LogicModel {

        RULE_WEIGHT("rule_weight", "【抽奖前规则】根据权重返回可抽奖范围", "before"),
        RULE_BLACKLIST("rule_blacklist", "【抽奖前规则】黑名单命中后返回指定奖品", "before"),
        RULE_LOCK("rule_lock", "【抽奖中规则】抽奖n次后，对应奖品可解锁抽奖", "center"),
        RULE_LUCK_AWARD("rule_luck_award", "【抽奖后规则】幸运奖兜底", "after"),
        ;

        private final String code;
        private final String info;
        private final String type;
    }

    @Getter
    @AllArgsConstructor
    public enum LogicType {

        BEFORE("before"),
        CENTER("center"),
        AFTER("after"),
        ;

        private final String code;
    }
}
