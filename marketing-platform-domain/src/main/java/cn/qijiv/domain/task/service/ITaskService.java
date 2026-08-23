package cn.qijiv.domain.task.service;


import cn.qijiv.domain.task.model.entity.TaskEntity;
import java.util.List;

/**
 * 任务服务接口
 *
 * @author qijiv
 * @since 2026/8/19
 */
public interface ITaskService {

    /**
     * 查询发送MQ失败和超时1分钟未发送的MQ
     *
     * @return 未发送的任务消息列表10条
     */
    List<TaskEntity> queryNoSendMessageTaskList();

    void sendMessage(TaskEntity taskEntity);

    void updateTaskSendMessageCompleted(String userId, String messageId);

    void updateTaskSendMessageFail(String userId, String messageId);

}
