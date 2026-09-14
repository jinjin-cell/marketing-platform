package cn.qijiv.trigger.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 日历签到记录响应DTO
 *
 * <p>日历上的签到标记统一以服务端数据为准，避免只存在浏览器本地导致换设备/换域名后丢失。
 *
 * @author qijiv
 * @since 2026/09/14
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CalendarSignRebateResponseDTO implements Serializable {

    /**
     * 服务端当前日期 yyyy-MM-dd，前端用于校准“今天”（签到业务日期以服务端为准）
     */
    private String serverDate;

    /**
     * 实际查询的起始日期 yyyy-MM-dd（含）
     */
    private String beginDate;

    /**
     * 实际查询的结束日期 yyyy-MM-dd（含）
     */
    private String endDate;

    /**
     * 区间内已签到的日期列表 yyyy-MM-dd，升序去重
     */
    private List<String> signDates;

}
