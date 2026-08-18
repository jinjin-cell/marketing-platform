package cn.qijiv.domain.strategy.model.entity;

import cn.qijiv.types.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 抽奖策略规则实体，记录策略规则或奖品规则配置，并提供权重规则解析能力。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyRuleEntity {

    /** 抽奖策略ID */
    private Long strategyId;
    /** 抽奖奖品ID；策略规则不需要奖品ID */
    private Integer awardId;
    /** 抽奖规则类型；1-策略规则、2-奖品规则 */
    private Integer ruleType;
    /** 抽奖规则模型 */
    private String ruleModel;
    /** 抽奖规则值 */
    private String ruleValue;
    /** 抽奖规则描述 */
    private String ruleDesc;

    /**
     * 将权重规则解析为“完整权重配置 -> 可抽奖品ID列表”。
     *
     * <p>输入示例：</p>
     * <pre>4000:102,103,104,105 5000:102,103,104,105,106,107</pre>
     *
     * <p>Map 的 key 保留完整权重配置，用于生成和查询同一套 Redis 概率表；
     * value 是该档位允许参与抽奖的奖品ID列表。</p>
     *
     * @return 权重配置与奖品ID列表的映射；非 rule_weight 规则返回空Map
     */
    public Map<String, List<Integer>> getRuleWeightValues() {
        if (!"rule_weight".equals(ruleModel)) {
            return Collections.emptyMap();
        }
        if (StringUtils.isBlank(ruleValue)) {
            throw new IllegalArgumentException("rule_weight ruleValue cannot be blank");
        }

        // 多个权重档位以任意连续空白字符分隔。
        Map<String, List<Integer>> resultMap = new LinkedHashMap<>();
        String[] ruleValueGroups = ruleValue.trim().split("\\s+");
        for (String ruleValueGroup : ruleValueGroups) {
            // 每一组格式固定为“权重值:奖品ID,奖品ID,...”。
            String[] parts = ruleValueGroup.split(Constants.COLON, 2);
            if (parts.length != 2 || StringUtils.isBlank(parts[0]) || StringUtils.isBlank(parts[1])) {
                throw new IllegalArgumentException("rule_weight invalid input format: " + ruleValueGroup);
            }

            // 冒号右侧转换为领域层使用的奖品ID集合。
            List<Integer> awardIds = new ArrayList<>();
            for (String value : parts[1].split(Constants.SPLIT)) {
                awardIds.add(Integer.parseInt(value.trim()));
            }
            // 保留完整分组字符串，确保装配和调度拼接出的key完全一致。
            resultMap.put(ruleValueGroup, awardIds);
        }
        return resultMap;
    }
}
