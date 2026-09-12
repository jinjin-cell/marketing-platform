package cn.qijiv.trigger.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户中奖记录响应DTO
 *
 * @author qijiv
 * @since 2026/09/12
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAwardRecordResponseDTO implements Serializable {

    /**
     * 奖品ID
     */
    private Integer awardId;

    /**
     * 奖品标题
     */
    private String awardTitle;

    /**
     * 中奖时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date awardTime;

    /**
     * 奖品状态；create-创建、complete-发奖完成、fail-发奖失败
     */
    private String awardState;

}
