package cn.qijiv.infrastructure.event;

import cn.qijiv.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;

/**
 * 事件发布器，负责将领域事件消息序列化为 JSON 并通过 RabbitMQ 发送。
 */
@Slf4j
@Component
public class EventPublisher {

    /** RabbitMQ 发送模板 */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发布事件消息到指定主题
     *
     * @param topic        消息主题
     * @param eventMessage 事件消息体
     */
    public void publish(String topic, BaseEvent.EventMessage<?> eventMessage) {
        try {
            String messageJson = JSON.toJSONString(eventMessage);
            rabbitTemplate.convertAndSend(topic, messageJson);
            log.info("发送MQ消息 topic:{} message:{}", topic, messageJson);
        } catch (Exception e) {
            log.error("发送MQ消息失败 topic:{} message:{}", topic, JSON.toJSONString(eventMessage), e);
            throw e;
        }
    }

}

