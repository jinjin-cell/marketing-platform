package cn.qijiv.trigger.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 活动信息响应DTO（活动列表）
 *
 * @author qijiv
 * @since 2026/09/14
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivityInfoResponseDTO implements Serializable {

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 活动描述
     */
    private String activityDesc;

    /**
     * 活动状态编码；create-创建、open-开启、close-关闭
     */
    private String state;

    /**
     * 活动状态说明
     */
    private String stateDesc;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date beginDateTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endDateTime;

}
