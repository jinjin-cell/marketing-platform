package cn.qijiv.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户积分账户持久化对象
 *
 * @author qijiv
 * @since 2026-08-26
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCreditAccountPO {

    /** 自增ID */
    private Long id;
    /** 用户ID */
    private String userId;
    /** 总积分 - 显示总账户值，一个人获得的总积分 */
    private BigDecimal totalAmount;
    /** 可用积分 - 每次扣减的值 */
    private BigDecimal availableAmount;
    /** 账户状态【open - 可用，close - 冻结】 */
    private String accountStatus;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

}
