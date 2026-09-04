package cn.qijiv.domain.credit.event;

import cn.qijiv.types.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 信积分账户调整成功消息【充值、支付，成功消息】
 *
 * @author qijiv
 * @since 2026/9/4
 */
@Component
public class CreditAdjustSuccessMessageEvent extends BaseEvent<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage> {

    @Value("${spring.rabbitmq.topic.credit_adjust_success}")
    private String topic;

    /**
     * 构建消息
     *
     * @param data 消息数据
     * @return 消息
     */
    @Override
    public EventMessage<CreditAdjustSuccessMessage> buildEventMessage(CreditAdjustSuccessMessage data) {
        return EventMessage.<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage>builder()
                .id(RandomStringUtils.randomNumeric(11))
                .timestamp(new Date())
                .data(data)
                .build();
    }

    /**
     * 获取主题
     *
     * @return 主题
     */
    @Override
    public String topic() {
        return topic;
    }

    /**
     * 信积分账户调整成功消息数据
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreditAdjustSuccessMessage {

        /**
         * 用户ID
         */
        private String userId;
        /**
         * 订单ID
         */
        private String orderId;
        /**
         * 交易金额
         */
        private BigDecimal amount;
        /** 交易名称编码，用于消费者区分返利和商品兑换。 */
        private String tradeName;
        /** 交易类型编码。 */
        private String tradeType;
        /**
         * 业务仿重ID - 外部透传。返利、行为等唯一标识
         */
        private String outBusinessNo;
    }

}
