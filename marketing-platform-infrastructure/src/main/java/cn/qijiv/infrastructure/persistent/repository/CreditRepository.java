package cn.qijiv.infrastructure.persistent.repository;

import cn.qijiv.domain.award.model.valobj.AccountStatusVO;
import cn.qijiv.domain.credit.model.aggregate.TradeAggregate;
import cn.qijiv.domain.credit.model.entity.CreditAccountEntity;
import cn.qijiv.domain.credit.model.entity.CreditOrderEntity;
import cn.qijiv.domain.credit.model.entity.TaskEntity;
import cn.qijiv.domain.credit.repository.ICreditRepository;
import cn.qijiv.infrastructure.event.EventPublisher;
import cn.qijiv.infrastructure.persistent.dao.ITaskDao;
import cn.qijiv.infrastructure.persistent.dao.IUserCreditAccountDao;
import cn.qijiv.infrastructure.persistent.dao.IUserCreditOrderDao;
import cn.qijiv.infrastructure.persistent.db.IDBRouterStrategy;
import cn.qijiv.infrastructure.persistent.po.TaskPO;
import cn.qijiv.infrastructure.persistent.po.UserCreditAccountPO;
import cn.qijiv.infrastructure.persistent.po.UserCreditOrderPO;
import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import cn.qijiv.types.common.Constants;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 用户积分调额仓储实现
 *
 * @author qijiv
 * @since 2026/9/3
 */
@Slf4j
@Repository
public class CreditRepository implements ICreditRepository {

    @Resource
    private IRedisService redisService;
    @Resource
    private IUserCreditAccountDao userCreditAccountDao;
    @Resource
    private IUserCreditOrderDao userCreditOrderDao;
    @Resource
    private ITaskDao taskDao;
    @Resource
    private IDBRouterStrategy dbRouter;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private EventPublisher eventPublisher;


    /** 保存积分交易、可靠消息任务，并在事务提交后尝试发布消息。 */
    @Override
    public String saveUserCreditTradeOrder(TradeAggregate tradeAggregate) {
        String userId = tradeAggregate.getUserId();
        CreditAccountEntity creditAccountEntity = tradeAggregate.getCreditAccountEntity();
        CreditOrderEntity creditOrderEntity = tradeAggregate.getCreditOrderEntity();
        TaskEntity taskEntity = tradeAggregate.getTaskEntity();

        // 积分账户
        UserCreditAccountPO userCreditAccountReq = new UserCreditAccountPO();
        userCreditAccountReq.setUserId(userId);
        userCreditAccountReq.setTotalAmount(creditAccountEntity.getAdjustAmount());
        // 知识；仓储往上有业务语义，仓储往下到 dao 操作是没有业务语义的。所以不用在乎这块使用的字段名称，直接用持久化对象即可。
        userCreditAccountReq.setAvailableAmount(creditAccountEntity.getAdjustAmount());

        // 积分订单
        UserCreditOrderPO userCreditOrderReq = new UserCreditOrderPO();
        userCreditOrderReq.setUserId(creditOrderEntity.getUserId());
        userCreditOrderReq.setOrderId(creditOrderEntity.getOrderId());
        userCreditOrderReq.setTradeName(creditOrderEntity.getTradeName().getName());
        userCreditOrderReq.setTradeType(creditOrderEntity.getTradeType().getCode());
        userCreditOrderReq.setTradeAmount(creditOrderEntity.getTradeAmount());
        userCreditOrderReq.setOutBusinessNo(creditOrderEntity.getOutBusinessNo());

        TaskPO task = new TaskPO();
        task.setUserId(taskEntity.getUserId());
        task.setTopic(taskEntity.getTopic());
        task.setMessageId(taskEntity.getMessageId());
        task.setMessage(JSON.toJSONString(taskEntity.getMessage()));
        task.setState(taskEntity.getState().getCode());

        RLock lock = redisService.getLock(Constants.RedisKey.USER_CREDIT_ACCOUNT_LOCK + userId);
        boolean locked = false;
        try {
            try {
                locked = lock.tryLock(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AppException(ResponseCode.CREDIT_ACCOUNT_LOCK_TIMEOUT.getCode(), ResponseCode.CREDIT_ACCOUNT_LOCK_TIMEOUT.getInfo());
            }
            if (!locked) {
                throw new AppException(ResponseCode.CREDIT_ACCOUNT_LOCK_TIMEOUT.getCode(), ResponseCode.CREDIT_ACCOUNT_LOCK_TIMEOUT.getInfo());
            }

            dbRouter.doRouter(userId);
            final boolean[] created = {false};
            String effectiveOrderId = transactionTemplate.execute(status -> {
                try {
                    UserCreditOrderPO existingOrder = userCreditOrderDao.queryUserCreditOrderByOutBusinessNo(userCreditOrderReq);
                    if (null != existingOrder) {
                        return existingOrder.getOrderId();
                    }

                    UserCreditAccountPO userCreditAccount = userCreditAccountDao.queryUserCreditAccountByUserId(userCreditAccountReq);
                    if (null == userCreditAccount) {
                        if (creditAccountEntity.getAdjustAmount().signum() < 0) {
                            throw new AppException(ResponseCode.ACCOUNT_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_QUOTA_ERROR.getInfo());
                        }
                        userCreditAccountReq.setAccountStatus(AccountStatusVO.open.getCode());
                        userCreditAccountDao.insert(userCreditAccountReq);
                    } else {
                        validateCreditAccount(userCreditAccount, creditAccountEntity.getAdjustAmount());
                        if (1 != userCreditAccountDao.updateAddAmount(userCreditAccountReq)) {
                            throw new AppException(ResponseCode.ACCOUNT_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_QUOTA_ERROR.getInfo());
                        }
                    }

                    userCreditOrderDao.insert(userCreditOrderReq);
                    taskDao.insert(task);
                    created[0] = true;
                    return creditOrderEntity.getOrderId();
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("调整账户积分额度异常，唯一索引冲突 userId:{} orderId:{}", userId, creditOrderEntity.getOrderId(), e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                } catch (RuntimeException e) {
                    status.setRollbackOnly();
                    log.error("调整账户积分额度失败 userId:{} orderId:{}", userId, creditOrderEntity.getOrderId(), e);
                    throw e;
                }
            });

            if (created[0]) {
                publishCreditAdjustMessage(task, taskEntity, creditOrderEntity);
            } else {
                log.info("调整账户积分额度，业务号已存在 userId:{} outBusinessNo:{} orderId:{}",
                        userId, creditOrderEntity.getOutBusinessNo(), effectiveOrderId);
            }
            return effectiveOrderId;
        } finally {
            dbRouter.clear();
            if (locked) {
                lock.unlock();
            }
        }
    }

    private void publishCreditAdjustMessage(TaskPO task, TaskEntity taskEntity, CreditOrderEntity creditOrderEntity) {
        try {
            eventPublisher.publish(task.getTopic(), taskEntity.getMessage());
            taskDao.updateTaskSendMessageCompleted(task);
            log.info("调整账户积分记录，发送MQ消息完成 userId:{} orderId:{} topic:{}",
                    task.getUserId(), creditOrderEntity.getOrderId(), task.getTopic());
        } catch (Exception e) {
            log.error("调整账户积分记录，发送MQ消息失败 userId:{} topic:{}", task.getUserId(), task.getTopic(), e);
            try {
                taskDao.updateTaskSendMessageFail(task);
            } catch (Exception updateException) {
                log.error("更新积分消息任务失败 userId:{} messageId:{}", task.getUserId(), task.getMessageId(), updateException);
            }
        }
    }

    private void validateCreditAccount(UserCreditAccountPO creditAccount, BigDecimal adjustAmount) {
        if (!AccountStatusVO.open.getCode().equals(creditAccount.getAccountStatus())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        if (adjustAmount.signum() < 0
                && creditAccount.getAvailableAmount().add(adjustAmount).signum() < 0) {
            throw new AppException(ResponseCode.ACCOUNT_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_QUOTA_ERROR.getInfo());
        }
    }

}
