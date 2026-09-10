package cn.qijiv.infrastructure.adapter.repository;

import cn.qijiv.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import cn.qijiv.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.qijiv.domain.rebate.model.entity.TaskEntity;
import cn.qijiv.domain.rebate.model.valobj.BehaviorTypeVO;
import cn.qijiv.domain.rebate.model.valobj.DailyBehaviorRebateVO;
import cn.qijiv.domain.rebate.repository.IBehaviorRebateRepository;
import cn.qijiv.infrastructure.event.EventPublisher;
import cn.qijiv.infrastructure.dao.IDailyBehaviorRebateDao;
import cn.qijiv.infrastructure.dao.ITaskDao;
import cn.qijiv.infrastructure.dao.IUserBehaviorRebateOrderDao;
import cn.qijiv.infrastructure.db.IDBRouterStrategy;
import cn.qijiv.infrastructure.dao.po.DailyBehaviorRebatePO;
import cn.qijiv.infrastructure.dao.po.TaskPO;
import cn.qijiv.infrastructure.dao.po.UserBehaviorRebateOrderPO;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 行为返利仓储实现
 *
 * @author qijiv
 * @since 2026-08-26
 */
@Slf4j
@Repository
public class BehaviorRebateRepository implements IBehaviorRebateRepository {

    @Resource
    private IDailyBehaviorRebateDao dailyBehaviorRebateDao;
    @Resource
    private IUserBehaviorRebateOrderDao userBehaviorRebateOrderDao;
    @Resource
    private ITaskDao taskDao;
    @Resource
    private IDBRouterStrategy dbRouter;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private EventPublisher eventPublisher;

    /**
     * 查询行为返利配置
     *
     * @param behaviorTypeVO 行为类型
     * @return 行为返利配置列表
     */
    @Override
    public List<DailyBehaviorRebateVO> queryDailyBehaviorRebateConfig(BehaviorTypeVO behaviorTypeVO) {
        List<DailyBehaviorRebatePO> dailyBehaviorRebates = dailyBehaviorRebateDao.queryDailyBehaviorRebateByBehaviorType(behaviorTypeVO.getCode());
        List<DailyBehaviorRebateVO> dailyBehaviorRebateVOS = new ArrayList<>(dailyBehaviorRebates.size());
        for (DailyBehaviorRebatePO dailyBehaviorRebate : dailyBehaviorRebates) {
            dailyBehaviorRebateVOS.add(DailyBehaviorRebateVO.builder()
                    .behaviorType(dailyBehaviorRebate.getBehaviorType())
                    .rebateDesc(dailyBehaviorRebate.getRebateDesc())
                    .rebateType(dailyBehaviorRebate.getRebateType())
                    .rebateConfig(dailyBehaviorRebate.getRebateConfig())
                    .build());
        }
        return dailyBehaviorRebateVOS;
    }

    /**
     * 保存用户行为返利记录
     *
     * @param userId                 用户ID
     * @param behaviorRebateAggregates 行为返利聚合对象列表
     */
    @Override
    public void saveUserRebateRecord(String userId, List<BehaviorRebateAggregate> behaviorRebateAggregates) {
        try {
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try {
                    // 插入用户行为返利订单表
                    for (BehaviorRebateAggregate behaviorRebateAggregate : behaviorRebateAggregates) {
                        BehaviorRebateOrderEntity behaviorRebateOrderEntity = behaviorRebateAggregate.getBehaviorRebateOrderEntity();
                        // 用户行为返利订单对象
                        UserBehaviorRebateOrderPO userBehaviorRebateOrder = new UserBehaviorRebateOrderPO();
                        userBehaviorRebateOrder.setUserId(behaviorRebateOrderEntity.getUserId());
                        userBehaviorRebateOrder.setOrderId(behaviorRebateOrderEntity.getOrderId());
                        userBehaviorRebateOrder.setBehaviorType(behaviorRebateOrderEntity.getBehaviorType());
                        userBehaviorRebateOrder.setRebateDesc(behaviorRebateOrderEntity.getRebateDesc());
                        userBehaviorRebateOrder.setRebateType(behaviorRebateOrderEntity.getRebateType());
                        userBehaviorRebateOrder.setRebateConfig(behaviorRebateOrderEntity.getRebateConfig());
                        userBehaviorRebateOrder.setOutBusinessNo(behaviorRebateOrderEntity.getOutBusinessNo());
                        userBehaviorRebateOrder.setBizId(behaviorRebateOrderEntity.getBizId());
                        userBehaviorRebateOrderDao.insert(userBehaviorRebateOrder);

                        // 任务对象
                        TaskEntity taskEntity = behaviorRebateAggregate.getTaskEntity();
                        TaskPO task = new TaskPO();
                        task.setUserId(taskEntity.getUserId());
                        task.setTopic(taskEntity.getTopic());
                        task.setMessageId(taskEntity.getMessageId());
                        task.setMessage(JSON.toJSONString(taskEntity.getMessage()));
                        task.setState(taskEntity.getState().getCode());
                        taskDao.insert(task);
                    }
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入返利记录，唯一索引冲突，用户ID：{}", userId, e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
            // 同步发送MQ消息
            for (BehaviorRebateAggregate behaviorRebateAggregate : behaviorRebateAggregates) {
                TaskEntity taskEntity = behaviorRebateAggregate.getTaskEntity();
                TaskPO task = new TaskPO();
                task.setUserId(taskEntity.getUserId());
                task.setMessageId(taskEntity.getMessageId());
                try {
                    // 发送消息【在事务外执行，如果失败还有任务补偿】
                    eventPublisher.publish(taskEntity.getTopic(), taskEntity.getMessage());
                    // 更新数据库记录，task 任务表
                    taskDao.updateTaskSendMessageCompleted(task);
                } catch (Exception e) {
                    log.error("写入返利记录，发送MQ消息失败，用户ID：{}，消息主题：{}",
                            userId, taskEntity.getTopic(), e);
                    taskDao.updateTaskSendMessageFail(task);
                }
            }
        } finally {
            dbRouter.clear();
        }

    }

    @Override
    public List<BehaviorRebateOrderEntity> queryOrderByOutBusinessNo(String userId, String outBusinessNo) {
        dbRouter.doRouter(userId);
        try {
            UserBehaviorRebateOrderPO request = new UserBehaviorRebateOrderPO();
            request.setUserId(userId);
            request.setOutBusinessNo(outBusinessNo);
            List<UserBehaviorRebateOrderPO> rows = userBehaviorRebateOrderDao.queryOrderByOutBusinessNo(request);
            List<BehaviorRebateOrderEntity> result = new ArrayList<>();
            if (rows == null) return result;
            for (UserBehaviorRebateOrderPO row : rows) {
                result.add(BehaviorRebateOrderEntity.builder().userId(row.getUserId()).orderId(row.getOrderId())
                        .behaviorType(row.getBehaviorType()).rebateDesc(row.getRebateDesc()).rebateType(row.getRebateType())
                        .rebateConfig(row.getRebateConfig()).outBusinessNo(row.getOutBusinessNo()).bizId(row.getBizId()).build());
            }
            return result;
        } finally {
            dbRouter.clear();
        }
    }

}
