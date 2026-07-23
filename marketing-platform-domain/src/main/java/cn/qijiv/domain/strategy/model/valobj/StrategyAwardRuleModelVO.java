package cn.qijiv.domain.strategy.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 奖品规则模型值对象。
 *
 * <p>VO（Value Object，值对象）只表达一组业务值。这里仅用于告诉领域服务：
 * 某个奖品是否绑定规则树，以及绑定的是哪一棵树。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyAwardRuleModelVO {

    /** 规则树业务ID，例如 tree_lock；为空表示该奖品不执行规则树。 */
    private String ruleModels;
}
