package cn.qijiv.infrastructure.dao.po;

import lombok.Data;

import java.util.Date;

/**
 * 抽奖活动次数配置持久化对象
 *
 * @author jinlujia
 * @since 2026-07-27
 */
@Data
public class RaffleActivityCountPO {

    /**
     * 自增ID
     */
    private Long id;

    /**
     * 活动次数编号
     */
    private Long activityCountId;

    /**
     * 总次数
     */
    private Integer totalCount;

    /**
     * 日次数
     */
    private Integer dayCount;

    /**
     * 月次数
     */
    private Integer monthCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
