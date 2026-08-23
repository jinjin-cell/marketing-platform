package cn.qijiv.domain.task.repository;

import cn.qijiv.domain.task.model.entity.TaskEntity;

import java.util.List;

/**
 * 任务仓库接口
 *
 * @author qijiv
 * @since 2026/8/19
 */
public interface ITaskRepository {

    List<TaskEntity> queryNoSendMessageTaskList();

    void sendMessage(TaskEntity taskEntity);

    void updateTaskSendMessageCompleted(String userId, String messageId);

    void updateTaskSendMessageFail(String userId, String messageId);

}

