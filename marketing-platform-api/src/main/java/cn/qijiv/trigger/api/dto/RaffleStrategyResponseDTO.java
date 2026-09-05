package cn.qijiv.trigger.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 抽奖响应参数
 *
 * @author qijiv
 * @since 2026-07-18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RaffleStrategyResponseDTO implements Serializable {

    /** 奖品ID。 */
    private Integer awardId;
    /** 策略奖品配置中的排序编号。 */
    private Integer awardIndex;
}
