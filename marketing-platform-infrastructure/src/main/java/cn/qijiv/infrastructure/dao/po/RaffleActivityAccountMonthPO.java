package cn.qijiv.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 抽奖活动用户月次数
 * @author jinlujia
 * @since 2026-07-27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RaffleActivityAccountMonthPO {

    private final static SimpleDateFormat dateFormatMonth = new SimpleDateFormat("yyyy-MM");

    /** 自增ID */
    private String id;
    /** 用户ID */
    private String userId;
    /** 活动ID */
    private Long activityId;
    /** 月（yyyy-mm） */
    private String month;
    /** 月次数 */
    private Integer monthCount;
    /** 月次数-剩余 */
    private Integer monthCountSurplus;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

    /**
     * 获取当前月
     */
    public static String currentMonth() {
        return dateFormatMonth.format(new Date());
    }


}

