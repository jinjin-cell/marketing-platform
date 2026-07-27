package cn.qijiv.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抽奖奖品列表查询响应参数
 *
 * @author qijiv
 * @since 2026-07-18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RaffleAwardListResponseDTO {

    /** 奖品ID。 */
    private Integer awardId;
    /** 奖品标题。 */
    private String awardTitle;
    /** 奖品副标题。 */
    private String awardSubTitle;
    /** 奖品在抽奖盘中的排序编号。 */
    private Integer sort;
}
