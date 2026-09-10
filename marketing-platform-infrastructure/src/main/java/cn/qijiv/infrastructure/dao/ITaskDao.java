package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.db.annotation.DBRouter;
import cn.qijiv.infrastructure.dao.po.TaskPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 任务数据访问层
 * @author jinlujia
 * @since 2026-07-27
 */
@Mapper
public interface ITaskDao {

    List<TaskPO> queryNoSendMessageTaskList();

    @DBRouter
    void updateTaskSendMessageCompleted(TaskPO task);

    @DBRouter
    void updateTaskSendMessageFail(TaskPO task);

    void insert(TaskPO task);
}
