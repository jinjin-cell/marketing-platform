package cn.qijiv.domain.credit.model.entity;

import cn.qijiv.domain.award.model.valobj.TaskStateVO;
import cn.qijiv.domain.credit.event.CreditAdjustSuccessMessageEvent;
import cn.qijiv.types.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务实体
 *
 * @author qijiv
 * @since 2026/9/4
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskEntity {

    /** 活动ID */
    private String userId;
    /** 消息主题 */
    private String topic;
    /** 消息编号 */
    private String messageId;
    /** 消息主体 */
    private BaseEvent.EventMessage<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage> message;
    /** 任务状态；create-创建、completed-完成、fail-失败 */
    private TaskStateVO state;

}
