package cn.qijiv.domain.strategy.model.entity;

import lombok.Data;

/**
 * 抽奖规则物料实体类
 *
 * @author jinlujia
 * @date 2026/07/18
 */
@Data
public class RuleMatterEntity {

    /** 用户ID */
   private String userId;
    /** 抽奖策略ID */
   private Long strategyId;
    /** 奖品ID */
   private Integer awardId;
    /** 抽奖规则模型 */
   private String ruleModel;

}
