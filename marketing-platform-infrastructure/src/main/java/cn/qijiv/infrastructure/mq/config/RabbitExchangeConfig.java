package cn.qijiv.infrastructure.mq.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 自定义交换机声明。
 *
 * <p>交换机已可由 RabbitMQ Management 预先创建；声明为 Spring Bean
 * 后，应用启动时会进行幂等校验，确保新环境也具备相同的 Exchange。</p>
 *
 * @author qijiv
 * @since 2026-08-26
 */
@Configuration
public class RabbitExchangeConfig {

    @Bean
    public DirectExchange sendAwardExchange(
            @Value("${spring.rabbitmq.topic.send_award}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    public DirectExchange sendRebateExchange(
            @Value("${spring.rabbitmq.topic.send_rebate}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    public Queue sendAwardQueue() {
        return new Queue("send.award.queue", true, false, false);
    }

    @Bean
    public Binding sendAwardBinding(
            @Qualifier("sendAwardQueue") Queue queue,
            @Qualifier("sendAwardExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(exchange.getName());
    }

    @Bean
    public Queue sendRebateQueue() {
        return new Queue("send.rebate.queue", true, false, false);
    }

    @Bean
    public Binding sendRebateBinding(
            @Qualifier("sendRebateQueue") Queue queue,
            @Qualifier("sendRebateExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(exchange.getName());
    }

}
