package cn.qijiv.infrastructure.persistent.po;

import lombok.Data;

import java.util.Date;

/**
 * 抽奖策略持久化对象
 *
 * @author jinlujia
 * @date 2026/07/18
 */
@Data
public class StrategyPO {

    /**
     * 自增ID
     */
    private Long id;

    /**
     * 抽奖策略ID
     */
    private Long strategyId;

    /**
     * 抽奖策略描述
     */
    private String strategyDesc;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 抽奖规则模型 rule_weight,rule_blacklist
     */
    private String ruleModels;

}
