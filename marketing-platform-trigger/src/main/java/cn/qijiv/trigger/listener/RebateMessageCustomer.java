package cn.qijiv.trigger.listener;

import cn.qijiv.domain.activity.model.entity.SkuRechargeEntity;
import cn.qijiv.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.qijiv.domain.credit.model.entity.TradeEntity;
import cn.qijiv.domain.credit.model.valobj.TradeNameVO;
import cn.qijiv.domain.credit.model.valobj.TradeTypeVO;
import cn.qijiv.domain.credit.service.ICreditAdjustService;
import cn.qijiv.domain.rebate.event.SendRebateMessageEvent;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.event.BaseEvent;
import cn.qijiv.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * @author qijiv
 * @since 2026/8/28
 * 监听用户行为返利消息
 */
@Slf4j
@Component
public class RebateMessageCustomer {

    @Value("${spring.rabbitmq.topic.send_rebate}")
    private String topic;
    @Resource
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;
    @Resource
    private ICreditAdjustService creditAdjustService;


    /**
     * 声明并绑定队列：生产者把返利消息投递到 direct 交换机 {@code send.rebate}（routingKey=send.rebate），
     * 队列必须显式绑定到该交换机，否则签到返利消息不会被消费，积分/抽奖次数不会到账。
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${spring.rabbitmq.topic.send_rebate}", durable = "true"),
            exchange = @Exchange(value = "${spring.rabbitmq.topic.send_rebate}", type = ExchangeTypes.DIRECT, durable = "true"),
            key = "${spring.rabbitmq.topic.send_rebate}"))
    public void listener(String message) {
        try {
            log.info("监听用户行为返利消息 topic: {} message: {}", topic, message);
            // 1. 转换消息
            BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage> eventMessage = JSON.parseObject(message, new TypeReference<BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage>>() {
            }.getType());
            if (null == eventMessage || null == eventMessage.getData()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "返利消息数据为空");
            }
            SendRebateMessageEvent.RebateMessage rebateMessage = eventMessage.getData();
            if (StringUtils.isBlank(rebateMessage.getUserId())
                    || StringUtils.isBlank(rebateMessage.getRebateType())
                    || StringUtils.isBlank(rebateMessage.getRebateConfig())
                    || StringUtils.isBlank(rebateMessage.getBizId())) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }

            // 2. 入账奖励
            switch (rebateMessage.getRebateType()) {
                case "sku":
                    SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
                    skuRechargeEntity.setUserId(rebateMessage.getUserId());
                    skuRechargeEntity.setSku(Long.valueOf(rebateMessage.getRebateConfig()));
                    skuRechargeEntity.setOutBusinessNo(rebateMessage.getBizId());
                    raffleActivityAccountQuotaService.createSkuRechargeOrder(skuRechargeEntity);
                    break;
                case "integral":
                    TradeEntity tradeEntity = new TradeEntity();
                    tradeEntity.setUserId(rebateMessage.getUserId());
                    tradeEntity.setTradeName(TradeNameVO.REBATE);
                    tradeEntity.setTradeType(TradeTypeVO.FORWARD);
                    tradeEntity.setAmount(new BigDecimal(rebateMessage.getRebateConfig()));
                    tradeEntity.setOutBusinessNo(rebateMessage.getBizId());
                    creditAdjustService.createOrder(tradeEntity);
                    break;
                default:
                    log.info("监听用户行为返利消息 - 非支持的奖励类型 topic: {} message: {}", topic, message);
                    return;
            }
        } catch (AppException e) {
            if (ResponseCode.INDEX_DUP.getCode().equals(e.getCode())) {
                log.warn("监听用户行为返利消息，消费重复 topic: {} message: {}", topic, message, e);
                return;
            }
            throw e;
        } catch (Exception e) {
            log.error("监听用户行为返利消息，消费失败 topic: {} message: {}", topic, message, e);
            throw e;
        }
    }

}
