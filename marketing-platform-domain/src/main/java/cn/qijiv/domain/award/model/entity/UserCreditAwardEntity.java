package cn.qijiv.domain.award.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


/**
 * 用户积分奖品实体类
 * @author qijiv
 * @since 2026/09/03
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCreditAwardEntity {

    /** 用户ID */
    private String userId;
    /** 积分值 */
    private BigDecimal creditAmount;

}

