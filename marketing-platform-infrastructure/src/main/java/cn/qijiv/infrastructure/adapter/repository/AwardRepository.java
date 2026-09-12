package cn.qijiv.infrastructure.adapter.repository;

import cn.qijiv.domain.award.model.aggregate.GiveOutPrizesAggregate;
import cn.qijiv.domain.award.model.aggregate.UserAwardRecordAggregate;
import cn.qijiv.domain.award.model.entity.TaskEntity;
import cn.qijiv.domain.award.model.entity.UserAwardRecordEntity;
import cn.qijiv.domain.award.model.entity.UserCreditAwardEntity;
import cn.qijiv.domain.award.model.valobj.AccountStatusVO;
import cn.qijiv.domain.award.model.valobj.AwardStateVO;
import cn.qijiv.domain.award.respository.IAwardRepository;
import cn.qijiv.infrastructure.dao.*;
import cn.qijiv.infrastructure.event.EventPublisher;
import cn.qijiv.infrastructure.db.IDBRouterStrategy;
import cn.qijiv.infrastructure.dao.po.TaskPO;
import cn.qijiv.infrastructure.dao.po.UserAwardRecordPO;
import cn.qijiv.infrastructure.dao.po.UserCreditAccountPO;
import cn.qijiv.infrastructure.dao.po.UserRaffleOrderPO;
import cn.qijiv.infrastructure.redis.IRedisService;
import cn.qijiv.types.common.Constants;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * 中奖记录仓储
 * @author qijiv
 * @since 2026-08-19
 */
@Slf4j
@Component
public class AwardRepository implements IAwardRepository {

    /** 奖品配置/奖品Key 缓存过期时间（分钟）。奖品配置相对稳定，变更后最长 30 分钟刷新。 */
    private static final long AWARD_CACHE_TTL_MINUTES = 30L;

    @Resource
    private IAwardDao awardDao;
    @Resource
    private ITaskDao taskDao;
    @Resource
    private IUserAwardRecordDao userAwardRecordDao;
    @Resource
    private IUserRaffleOrderDao userRaffleOrderDao;
    @Resource
    private IUserCreditAccountDao userCreditAccountDao;
    @Resource
    private IDBRouterStrategy dbRouter;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private EventPublisher eventPublisher;
    @Resource
    private IRedisService redisService;

    @Override
    public void saveUserAwardRecord(UserAwardRecordAggregate userAwardRecordAggregate) {

        UserAwardRecordEntity userAwardRecordEntity = userAwardRecordAggregate.getUserAwardRecordEntity();
        TaskEntity taskEntity = userAwardRecordAggregate.getTaskEntity();
        String userId = userAwardRecordEntity.getUserId();
        Long activityId = userAwardRecordEntity.getActivityId();
        Integer awardId = userAwardRecordEntity.getAwardId();

        UserAwardRecordPO userAwardRecord = new UserAwardRecordPO();
        userAwardRecord.setUserId(userAwardRecordEntity.getUserId());
        userAwardRecord.setActivityId(userAwardRecordEntity.getActivityId());
        userAwardRecord.setStrategyId(userAwardRecordEntity.getStrategyId());
        userAwardRecord.setOrderId(userAwardRecordEntity.getOrderId());
        userAwardRecord.setAwardId(userAwardRecordEntity.getAwardId());
        userAwardRecord.setAwardTitle(userAwardRecordEntity.getAwardTitle());
        userAwardRecord.setAwardTime(userAwardRecordEntity.getAwardTime());
        userAwardRecord.setAwardState(userAwardRecordEntity.getAwardState().getCode());

        TaskPO task = new TaskPO();
        task.setUserId(taskEntity.getUserId());
        task.setTopic(taskEntity.getTopic());
        task.setMessageId(taskEntity.getMessageId());
        task.setMessage(JSON.toJSONString(taskEntity.getMessage()));
        task.setState(taskEntity.getState().getCode());

        UserRaffleOrderPO  userRaffleOrderReq = new UserRaffleOrderPO();
        userRaffleOrderReq.setUserId(userAwardRecordEntity.getUserId());
        userRaffleOrderReq.setOrderId(userAwardRecordEntity.getOrderId());

        try {
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try {
                    // 写入记录
                    userAwardRecordDao.insert(userAwardRecord);
                    // 写入任务
                    taskDao.insert(task);
                    //更新抽奖单
                    int count = userRaffleOrderDao.updateUserRaffleOrderStateUsed(userRaffleOrderReq);
                    if (1 != count) {
                        status.setRollbackOnly();
                        log.error("写入中奖记录，用户抽奖单已使用过，不可重复抽奖 userId: {} activityId: {} awardId: {}", userId, activityId, awardId);
                        throw new AppException(ResponseCode.ACTIVITY_ORDER_ERROR.getCode(), ResponseCode.ACTIVITY_ORDER_ERROR.getInfo());
                    }

                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入中奖记录，唯一索引冲突 userId: {} activityId: {} awardId: {}", userId, activityId, awardId, e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });

            // 发送消息【在事务外执行，如果失败还有任务补偿】
            eventPublisher.publish(task.getTopic(), task.getMessage());
            // 更新数据库记录，task 任务表
            taskDao.updateTaskSendMessageCompleted(task);
        } catch (Exception e) {
            log.error("写入中奖记录，发送MQ消息失败 userId: {} topic: {}", userId, task.getTopic());
            taskDao.updateTaskSendMessageFail(task);
        } finally {
            dbRouter.clear();
        }

    }

    @Override
    public String queryAwardConfig(Integer awardId) {
        String cacheKey = Constants.RedisKey.AWARD_CONFIG_KEY + awardId;
        String cachedValue = redisService.getValue(cacheKey);
        if (null != cachedValue) {
            return cachedValue;
        }

        String value = awardDao.queryAwardConfigByAwardId(awardId);
        // 仅缓存正常结果，避免空值缓存穿透
        if (null != value) {
            redisService.setValue(cacheKey, value, AWARD_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return value;
    }

    @Override
    public void saveGiveOutPrizesAggregate(GiveOutPrizesAggregate giveOutPrizesAggregate) {
        String userId = giveOutPrizesAggregate.getUserId();
        UserCreditAwardEntity userCreditAwardEntity = giveOutPrizesAggregate.getUserCreditAwardEntity();
        UserAwardRecordEntity userAwardRecordEntity = giveOutPrizesAggregate.getUserAwardRecordEntity();

        // 更新发奖记录
        UserAwardRecordPO userAwardRecordReq = new UserAwardRecordPO();
        userAwardRecordReq.setUserId(userId);
        userAwardRecordReq.setOrderId(userAwardRecordEntity.getOrderId());
        userAwardRecordReq.setAwardState(userAwardRecordEntity.getAwardState().getCode());

        // 更新用户积分 「首次则插入数据」
        UserCreditAccountPO userCreditAccountReq = new UserCreditAccountPO();
        userCreditAccountReq.setUserId(userCreditAwardEntity.getUserId());
        userCreditAccountReq.setTotalAmount(userCreditAwardEntity.getCreditAmount());
        userCreditAccountReq.setAvailableAmount(userCreditAwardEntity.getCreditAmount());
        userCreditAccountReq.setAccountStatus(AccountStatusVO.open.getCode());

        try {
            dbRouter.doRouter(giveOutPrizesAggregate.getUserId());
            transactionTemplate.execute(status -> {
                try {
                    // 更新积分 || 创建积分账户
                    int updateAccountCount = userCreditAccountDao.updateAddAmount(userCreditAccountReq);
                    if (0 == updateAccountCount) {
                        userCreditAccountDao.insert(userCreditAccountReq);
                    }

                    // 更新奖品记录
                    int updateAwardCount = userAwardRecordDao.updateAwardRecordCompletedState(userAwardRecordReq);
                    if (0 == updateAwardCount) {
                        log.warn("更新中奖记录，重复更新拦截 userId:{} giveOutPrizesAggregate:{}", userId, JSON.toJSONString(giveOutPrizesAggregate));
                        status.setRollbackOnly();
                    }
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("更新中奖记录，唯一索引冲突 userId: {} ", userId, e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
        } finally {
            dbRouter.clear();
        }
    }

    @Override
    public List<UserAwardRecordEntity> queryUserAwardRecordList(String userId, Long activityId) {
        UserAwardRecordPO userAwardRecordReq = new UserAwardRecordPO();
        userAwardRecordReq.setUserId(userId);
        userAwardRecordReq.setActivityId(activityId);
        // user_award_record 由 ShardingSphere 按 user_id 分库分表，查询条件携带 user_id 即可自动路由
        List<UserAwardRecordPO> userAwardRecordPOS = userAwardRecordDao.queryUserAwardRecordList(userAwardRecordReq);
        List<UserAwardRecordEntity> userAwardRecordEntities = new ArrayList<>(userAwardRecordPOS.size());
        for (UserAwardRecordPO userAwardRecordPO : userAwardRecordPOS) {
            userAwardRecordEntities.add(UserAwardRecordEntity.builder()
                    .userId(userAwardRecordPO.getUserId())
                    .activityId(userAwardRecordPO.getActivityId())
                    .strategyId(userAwardRecordPO.getStrategyId())
                    .orderId(userAwardRecordPO.getOrderId())
                    .awardId(userAwardRecordPO.getAwardId())
                    .awardTitle(userAwardRecordPO.getAwardTitle())
                    .awardTime(userAwardRecordPO.getAwardTime())
                    .awardState(AwardStateVO.valueOf(userAwardRecordPO.getAwardState()))
                    .build());
        }
        return userAwardRecordEntities;
    }

    @Override
    public String queryAwardKey(Integer awardId) {
        String cacheKey = Constants.RedisKey.AWARD_KEY + awardId;
        String cachedValue = redisService.getValue(cacheKey);
        if (null != cachedValue) {
            return cachedValue;
        }

        String value = awardDao.queryAwardKeyByAwardId(awardId);
        // 仅缓存正常结果，避免空值缓存穿透
        if (null != value) {
            redisService.setValue(cacheKey, value, AWARD_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return value;
    }

}




