package cn.qijiv.trigger.job;

import cn.qijiv.domain.task.model.entity.TaskEntity;
import cn.qijiv.domain.task.service.ITaskService;
import cn.qijiv.infrastructure.db.IDBRouterStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/*
* 定时任务，发送MQ消息
* @author qijiv
* @since 2026/8/19
* */
@Slf4j
@Component()
public class SendMessageTaskJob {

    @Resource
    private ITaskService taskService;
    @Resource
    private ThreadPoolExecutor executor;
    @Resource
    private IDBRouterStrategy dbRouter;

    @Scheduled(cron = "0/5 * * * * ?")
    public void exec() {
        try {
            // 获取分库数量
            int dbCount = dbRouter.dbCount();

            // 逐个库扫描表【每个库一个任务表】
            for (int dbIdx = 1; dbIdx <= dbCount; dbIdx++) {
                int finalDbIdx = dbIdx;
                executor.execute(() -> {
                    try {
                        dbRouter.setDBKey(finalDbIdx);
                        dbRouter.setTBKey(0);
                        List<TaskEntity> taskEntities = taskService.queryNoSendMessageTaskList();
                        if (taskEntities.isEmpty()) return;
                        // 发送MQ消息
                        for (TaskEntity taskEntity : taskEntities) {
                            // 开启线程发送，提高发送效率。配置的线程池策略为 CallerRunsPolicy，在 ThreadPoolConfig 配置中有4个策略，面试中容易对比提问。可以检索下相关资料。
                            executor.execute(() -> {
                                try {
                                    // 新线程需要重新设置路由，HintManager 为 ThreadLocal 存储，无法跨线程传递。
                                    // 任务是在 ds{finalDbIdx} 库扫描出来的，状态更新必须沿用同一个库路由，不能按 userId 重算，
                                    // 否则会路由到别的库导致任务状态永远更新不到、消息无限重发。
                                    dbRouter.setDBKey(finalDbIdx);
                                    taskService.sendMessage(taskEntity);
                                    taskService.updateTaskSendMessageCompleted(taskEntity.getUserId(), taskEntity.getMessageId());
                                } catch (Exception e) {
                                    log.error("定时任务，发送MQ消息失败 userId: {} topic: {}", taskEntity.getUserId(), taskEntity.getTopic());
                                    taskService.updateTaskSendMessageFail(taskEntity.getUserId(), taskEntity.getMessageId());
                                } finally {
                                    dbRouter.clear();
                                }
                            });
                        }
                    } finally {
                        dbRouter.clear();
                    }
                });
            }
        } catch (Exception e) {
            log.error("定时任务，扫描MQ任务表发送消息失败。", e);
        } finally {
            dbRouter.clear();
        }
    }

}

