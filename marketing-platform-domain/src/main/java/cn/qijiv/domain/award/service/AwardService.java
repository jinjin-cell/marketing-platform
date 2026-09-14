package cn.qijiv.domain.award.service;

import cn.qijiv.domain.award.event.SendAwardMessageEvent;
import cn.qijiv.domain.award.model.aggregate.UserAwardRecordAggregate;
import cn.qijiv.domain.award.model.entity.DistributeAwardEntity;
import cn.qijiv.domain.award.model.entity.TaskEntity;
import cn.qijiv.domain.award.model.entity.UserAwardRecordEntity;
import cn.qijiv.domain.award.model.valobj.TaskStateVO;
import cn.qijiv.domain.award.respository.IAwardRepository;
import cn.qijiv.domain.award.service.distribute.IDistributeAward;
import cn.qijiv.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 奖品服务类
 *
 * @author qijiv
 * @since 2026-08-19
 */
@Slf4j
@Service
public class AwardService implements IAwardService {

    private final IAwardRepository awardRepository;
    private final SendAwardMessageEvent sendAwardMessageEvent;
    private final Map<String, IDistributeAward> distributeAwardMap;

    public AwardService(IAwardRepository awardRepository, SendAwardMessageEvent sendAwardMessageEvent, Map<String, IDistributeAward> distributeAwardMap) {
        this.awardRepository = awardRepository;
        this.sendAwardMessageEvent = sendAwardMessageEvent;
        this.distributeAwardMap = distributeAwardMap;
    }

    @Override
    public void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity) {
        // 构建消息对象
        SendAwardMessageEvent.SendAwardMessage sendAwardMessage = new SendAwardMessageEvent.SendAwardMessage();
        sendAwardMessage.setUserId(userAwardRecordEntity.getUserId());
        sendAwardMessage.setOrderId(userAwardRecordEntity.getOrderId());
        sendAwardMessage.setAwardId(userAwardRecordEntity.getAwardId());
        sendAwardMessage.setAwardTitle(userAwardRecordEntity.getAwardTitle());
        sendAwardMessage.setAwardConfig(userAwardRecordEntity.getAwardConfig());

        BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage> sendAwardMessageEventMessage = sendAwardMessageEvent.buildEventMessage(sendAwardMessage);

        // 构建任务对象
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setUserId(userAwardRecordEntity.getUserId());
        taskEntity.setTopic(sendAwardMessageEvent.topic());
        taskEntity.setMessageId(sendAwardMessageEventMessage.getId());
        taskEntity.setMessage(sendAwardMessageEventMessage);
        taskEntity.setState(TaskStateVO.create);

        // 构建聚合对象
        UserAwardRecordAggregate userAwardRecordAggregate = UserAwardRecordAggregate.builder()
                .taskEntity(taskEntity)
                .userAwardRecordEntity(userAwardRecordEntity)
                .build();

        // 存储聚合对象 - 一个事务下，用户的中奖记录
        awardRepository.saveUserAwardRecord(userAwardRecordAggregate);
    }

    @Override
    public List<UserAwardRecordEntity> queryUserAwardRecordList(String userId, Long activityId) {
        return awardRepository.queryUserAwardRecordList(userId, activityId);
    }

    @Override
    public void distributeAward(DistributeAwardEntity distributeAwardEntity) {
        // 奖品Key
        String awardKey = awardRepository.queryAwardKey(distributeAwardEntity.getAwardId());
        if (null == awardKey) {
            // 消息可以正常确认，但中奖记录必须保持 create，等待人工履约。
            log.warn("分发奖品，奖品未配置 awardKey，保留为待发放 userId:{} orderId:{} awardId:{}",
                    distributeAwardEntity.getUserId(), distributeAwardEntity.getOrderId(), distributeAwardEntity.getAwardId());
            return;
        }

        // 奖品服务
        IDistributeAward distributeAward = distributeAwardMap.get(awardKey);

        if (null == distributeAward) {
            // 本项目目前只实现积分类奖品自动发放。未知实现不抛异常，避免 MQ 无限重投，
            // 同时不伪造完成态，保留 create 供人工履约。
            log.warn("分发奖品，未实现该 awardKey 的发放逻辑，保留为待发放 userId:{} orderId:{} awardId:{} awardKey:{}",
                    distributeAwardEntity.getUserId(), distributeAwardEntity.getOrderId(), distributeAwardEntity.getAwardId(), awardKey);
            return;
        }

        // 发放奖品
        distributeAward.giveOutPrizes(distributeAwardEntity);
    }

}
