package cn.qijiv.infrastructure.event;

import cn.qijiv.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    /** 发奖自定义交换机名称 */
    @Value("${spring.rabbitmq.topic.send_award}")
    private String sendAwardExchange;

    /** 返利自定义交换机名称 */
    @Value("${spring.rabbitmq.topic.send_rebate}")
    private String sendRebateExchange;

    /**
     * 发布事件消息到指定主题
     *
     * @param topic        消息主题
     * @param eventMessage 事件消息体
     */
    public void publish(String topic, BaseEvent.EventMessage<?> eventMessage) {
        try {
            String messageJson = JSON.toJSONString(eventMessage);
            publish(topic, messageJson);
        } catch (Exception e) {
            log.error("发送MQ消息失败 topic:{} message:{}", topic, JSON.toJSONString(eventMessage), e);
            throw e;
        }
    }


    /**
     * 发布事件消息到指定主题
     *
     * @param topic        消息主题
     * @param eventMessageJSON 事件消息体JSON字符串
     */
    public void publish(String topic, String eventMessageJSON){
        try {
            // 历史 topic 字段同时用于库存队列和奖品/返利任务。只有自定义
            // Exchange 使用显式 exchange + routing key，其它消息继续走默认交换机。
            String exchange = resolveExchange(topic);
            if (null != exchange) {
                publishToExchange(exchange, exchange, eventMessageJSON);
            } else {
                rabbitTemplate.convertAndSend(topic, eventMessageJSON);
                log.info("发送MQ消息 exchange:(default) routingKey:{} message:{}", topic, eventMessageJSON);
            }
        } catch (Exception e) {
            log.error("发送MQ消息失败 topic:{} message:{}", topic, eventMessageJSON, e);
            throw e;
        }
    }

    /**
     * 发布消息到指定 Exchange。
     *
     * @param exchange Exchange 名称
     * @param routingKey 路由键
     * @param eventMessageJSON 消息 JSON
     */
    public void publishToExchange(String exchange, String routingKey, String eventMessageJSON) {
        rabbitTemplate.convertAndSend(exchange, routingKey, eventMessageJSON);
        log.info("发送MQ消息 exchange:{} routingKey:{} message:{}", exchange, routingKey, eventMessageJSON);
    }

    /**
     * 判断是否为项目使用的自定义 Exchange。
     * 该判断保证 task 表中的历史 topic 在补偿发送时仍能正确路由。
     */
    private String resolveExchange(String topic) {
        // 兼容旧任务表中保存的下划线主题值，新消息统一走点号 Exchange。
        if (sendAwardExchange.equals(topic) || "send_award".equals(topic)) {
            return sendAwardExchange;
        }
        if (sendRebateExchange.equals(topic) || "send_rebate".equals(topic)) {
            return sendRebateExchange;
        }
        return null;
    }


}

