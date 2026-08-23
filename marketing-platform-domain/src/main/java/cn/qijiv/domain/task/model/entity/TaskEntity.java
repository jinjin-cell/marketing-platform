package cn.qijiv.domain.task.model.entity;

import lombok.Data;

/**
 * 任务实体类
 * @author qijiv
 * @since 2026/8/19
 */
@Data
public class TaskEntity {

    /** 活动ID */
    private String userId;
    /** 消息主题 */
    private String topic;
    /** 消息编号 */
    private String messageId;
    /** 消息主体 */
    private String message;

}
