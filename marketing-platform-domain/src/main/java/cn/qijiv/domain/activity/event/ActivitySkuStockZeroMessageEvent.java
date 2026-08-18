package cn.qijiv.domain.activity.event;

import cn.qijiv.types.event.BaseEvent;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 活动SKU库存为0消息事件
 */
@Component
public class ActivitySkuStockZeroMessageEvent extends BaseEvent<Long> {

    /** 活动SKU库存为0的MQ主题 */
    @Value("${spring.rabbitmq.topic.activity_sku_stock_zero}")
    private String topic;

    /**
     * 构建活动SKU库存为0的MQ消息体
     *
     * @param sku 活动商品SKU
     * @return 消息体
     */
    @Override
    public EventMessage<Long> buildEventMessage(Long sku) {
        return EventMessage.<Long>builder()
                .id(RandomStringUtils.randomNumeric(11))
                .timestamp(new Date())
                .data(sku)
                .build();
    }

    /**
     * 获取MQ主题
     *
     * @return 主题
     */
    @Override
    public String topic() {
        return topic;
    }

}

