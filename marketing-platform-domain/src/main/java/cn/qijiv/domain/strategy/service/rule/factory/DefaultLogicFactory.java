package cn.qijiv.domain.strategy.service.rule.factory;

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

    private final Map<String, ILogicFilter<RuleActionEntity.RaffleBeforeEntity>> logicFilterMap;

    public DefaultLogicFactory(List<ILogicFilter<RuleActionEntity.RaffleBeforeEntity>> logicFilters) {
        Map<String, ILogicFilter<RuleActionEntity.RaffleBeforeEntity>> filters = new LinkedHashMap<>();
        for (ILogicFilter<RuleActionEntity.RaffleBeforeEntity> logicFilter : logicFilters) {
            // Spring 注入全部过滤器后，通过注解建立“规则模型 -> 过滤器”的路由表。
            LogicStrategy strategy = AnnotationUtils.findAnnotation(logicFilter.getClass(), LogicStrategy.class);
            if (strategy == null) {
                continue;
            }
            String ruleModel = strategy.logicMode().getCode();
            if (filters.put(ruleModel, logicFilter) != null) {
                throw new IllegalStateException("重复的抽奖规则过滤器，ruleModel: " + ruleModel);
            }
        }
        // 构造完成后禁止外部修改，保证运行期间规则路由稳定。
        this.logicFilterMap = Collections.unmodifiableMap(filters);
    }

    public Map<String, ILogicFilter<RuleActionEntity.RaffleBeforeEntity>> openLogicFilter() {
        return logicFilterMap;
    }

    @Getter
    @AllArgsConstructor
    public enum LogicModel {

        RULE_WEIGHT("rule_weight", "【抽奖前规则】根据权重返回可抽奖范围"),
        RULE_BLACKLIST("rule_blacklist", "【抽奖前规则】黑名单命中后返回指定奖品"),
        ;

        private final String code;
        private final String info;
    }
}
