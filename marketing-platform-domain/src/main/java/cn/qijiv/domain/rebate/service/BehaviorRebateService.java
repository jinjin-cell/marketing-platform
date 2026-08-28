package cn.qijiv.domain.rebate.service;

import cn.qijiv.domain.rebate.event.SendRebateMessageEvent;
import cn.qijiv.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import cn.qijiv.domain.rebate.model.entity.BehaviorEntity;
import cn.qijiv.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.qijiv.domain.rebate.model.entity.TaskEntity;
import cn.qijiv.domain.rebate.model.valobj.DailyBehaviorRebateVO;
import cn.qijiv.domain.rebate.model.valobj.TaskStateVO;
import cn.qijiv.domain.rebate.repository.IBehaviorRebateRepository;
import cn.qijiv.types.common.Constants;
import cn.qijiv.types.event.BaseEvent;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 行为返利服务实现
 *
 * @author qijiv
 * @since 2026-08-26
 */
@Service
public class BehaviorRebateService implements IBehaviorRebateService {

    @Resource
    private IBehaviorRebateRepository behaviorRebateRepository;
    @Resource
    private SendRebateMessageEvent sendRebateMessageEvent;

    /**
     * 创建返利订单
     *
     * @param behaviorEntity 行为实体
     * @return 订单ID列表
     */
    @Override
    public List<String> createOrder(BehaviorEntity behaviorEntity) {
        if (behaviorEntity == null
                || StringUtils.isBlank(behaviorEntity.getUserId())
                || behaviorEntity.getBehaviorTypeVO() == null
                || StringUtils.isBlank(behaviorEntity.getOutBusinessNo())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        // 查询行为返利配置
        List<DailyBehaviorRebateVO> rebateConfigs = behaviorRebateRepository
                .queryDailyBehaviorRebateConfig(behaviorEntity.getBehaviorTypeVO());
        if (null == rebateConfigs || rebateConfigs.isEmpty()) {
            return new ArrayList<>();
        }

        //初始化订单号和聚合列表
        List<String> orderIds = new ArrayList<>(rebateConfigs.size());
        List<BehaviorRebateAggregate> aggregates = new ArrayList<>(rebateConfigs.size());
        // 遍历返利配置，创建订单和任务
        for (DailyBehaviorRebateVO rebateConfig : rebateConfigs) {
            String bizId = behaviorEntity.getUserId()
                    + Constants.UNDERLINE + rebateConfig.getRebateType()
                    + Constants.UNDERLINE + behaviorEntity.getOutBusinessNo();

            BehaviorRebateOrderEntity rebateOrder = BehaviorRebateOrderEntity.builder()
                    .userId(behaviorEntity.getUserId())
                    .orderId(RandomStringUtils.randomNumeric(12))
                    .behaviorType(rebateConfig.getBehaviorType())
                    .rebateDesc(rebateConfig.getRebateDesc())
                    .rebateType(rebateConfig.getRebateType())
                    .rebateConfig(rebateConfig.getRebateConfig())
                    .bizId(bizId)
                    .build();
            orderIds.add(rebateOrder.getOrderId());

            SendRebateMessageEvent.RebateMessage rebateMessage = SendRebateMessageEvent.RebateMessage.builder()
                    .userId(behaviorEntity.getUserId())
                    .rebateDesc(rebateConfig.getRebateDesc())
                    .rebateType(rebateConfig.getRebateType())
                    .rebateConfig(rebateConfig.getRebateConfig())
                    .bizId(bizId)
                    .build();
            BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage> eventMessage =
                    sendRebateMessageEvent.buildEventMessage(rebateMessage);

            TaskEntity taskEntity = TaskEntity.builder()
                    .userId(behaviorEntity.getUserId())
                    .topic(sendRebateMessageEvent.topic())
                    .messageId(eventMessage.getId())
                    .message(eventMessage)
                    .state(TaskStateVO.create)
                    .build();

            aggregates.add(BehaviorRebateAggregate.builder()
                    .userId(behaviorEntity.getUserId())
                    .behaviorRebateOrderEntity(rebateOrder)
                    .taskEntity(taskEntity)
                    .build());
        }

        behaviorRebateRepository.saveUserRebateRecord(behaviorEntity.getUserId(), aggregates);
        return orderIds;
    }

}
