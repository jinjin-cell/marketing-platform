package cn.qijiv.domain.credit.model.aggregate;

import cn.qijiv.domain.award.model.valobj.TaskStateVO;
import cn.qijiv.domain.credit.event.CreditAdjustSuccessMessageEvent;
import cn.qijiv.domain.credit.model.entity.CreditAccountEntity;
import cn.qijiv.domain.credit.model.entity.CreditOrderEntity;
import cn.qijiv.domain.credit.model.entity.TaskEntity;
import cn.qijiv.domain.credit.model.valobj.TradeNameVO;
import cn.qijiv.domain.credit.model.valobj.TradeTypeVO;
import cn.qijiv.types.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

import java.math.BigDecimal;

/**
 * 积分交易聚合实体类
 * @author qijiv
 * @since 2026/9/3
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeAggregate {

    // 用户ID
    private String userId;
    // 积分账户实体
    private CreditAccountEntity creditAccountEntity;
    // 积分订单实体
    private CreditOrderEntity creditOrderEntity;
    // 任务实体
    private TaskEntity taskEntity;

    /**
     * 创建积分账户实体
     * @param userId 用户ID
     * @param adjustAmount 积分调整金额
     * @return 积分账户实体
     */
    public static CreditAccountEntity createCreditAccountEntity(String userId, BigDecimal adjustAmount) {
        return CreditAccountEntity.builder().userId(userId).adjustAmount(adjustAmount).build();
    }

    /**
     * 创建积分订单实体
     * @param userId 用户ID
     * @param tradeName 积分交易名称
     * @param tradeType 积分交易类型
     * @param tradeAmount 积分交易金额
     * @param outBusinessNo 外部业务号
     * @return 积分订单实体
     */
    public static CreditOrderEntity createCreditOrderEntity(String userId,
                                                            TradeNameVO tradeName,
                                                            TradeTypeVO tradeType,
                                                            BigDecimal tradeAmount,
                                                            String outBusinessNo) {
        return CreditOrderEntity.builder()
                .userId(userId)
                .orderId(RandomStringUtils.randomNumeric(12))
                .tradeName(tradeName)
                .tradeType(tradeType)
                .tradeAmount(tradeAmount)
                .outBusinessNo(outBusinessNo)
                .build();
    }

    /**
     * 创建任务实体
     * @param userId 用户ID
     * @param topic 主题
     * @param messageId 消息ID
     * @param message 消息
     * @return 任务实体
     */
    public static TaskEntity createTaskEntity(String userId, String topic, String messageId, BaseEvent.EventMessage<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage> message) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setUserId(userId);
        taskEntity.setTopic(topic);
        taskEntity.setMessageId(messageId);
        taskEntity.setMessage(message);
        taskEntity.setState(TaskStateVO.create);
        return taskEntity;
    }


}

