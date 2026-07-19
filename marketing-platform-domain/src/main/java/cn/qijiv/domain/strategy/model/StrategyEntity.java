package cn.qijiv.domain.strategy.model;

import cn.qijiv.types.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyEntity {

    /** 抽奖策略ID */
    private Long strategyId;
    /** 抽奖策略描述 */
    private String strategyDesc;
    /** 抽奖规则模型 rule_weight,rule_blacklist */
    private String ruleModels;

    /**
     * 将策略配置的规则模型拆分为独立规则名。
     *
     * @return 规则模型数组；未配置时返回空数组
     */
    public String[] ruleModels() {
        if (StringUtils.isBlank(ruleModels)) return new String[0];
        return ruleModels.split(Constants.SPLIT);
    }

    /**
     * 查找策略是否声明了权重规则。
     *
     * @return rule_weight；未声明时返回null
     */
    public String getRuleWeight() {
        String[] ruleModels = this.ruleModels();
        for (String ruleModel : ruleModels) {
            if ("rule_weight".equals(ruleModel.trim())) return ruleModel.trim();
        }
        return null;
    }

}
