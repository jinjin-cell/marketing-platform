package cn.qijiv.infrastructure.persistent.repository;

import cn.qijiv.domain.award.model.valobj.AccountStatusVO;
import cn.qijiv.domain.credit.model.aggregate.TradeAggregate;
import cn.qijiv.domain.credit.model.entity.CreditAccountEntity;
import cn.qijiv.domain.credit.model.entity.CreditOrderEntity;
import cn.qijiv.domain.credit.repository.ICreditRepository;
import cn.qijiv.infrastructure.persistent.dao.IUserCreditAccountDao;
import cn.qijiv.infrastructure.persistent.dao.IUserCreditOrderDao;
import cn.qijiv.infrastructure.persistent.db.IDBRouterStrategy;
import cn.qijiv.infrastructure.persistent.po.UserCreditAccountPO;
import cn.qijiv.infrastructure.persistent.po.UserCreditOrderPO;
import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import cn.qijiv.types.common.Constants;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
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
    private IDBRouterStrategy dbRouter;
    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 保存用户积分交易：以用户维度加分布式锁，串行化同一用户的并发调额。
     * 幂等规则：同一 outBusinessNo 已有订单时直接返回原订单号，不重复增减账户。
     *
     * @param tradeAggregate 积分交易聚合
     * @return 生效的积分订单号（新建或已存在的原单号）
     */
    @Override
    public String saveUserCreditTradeOrder(TradeAggregate tradeAggregate) {
        String userId = tradeAggregate.getUserId();
        CreditAccountEntity creditAccountEntity = tradeAggregate.getCreditAccountEntity();
        CreditOrderEntity creditOrderEntity = tradeAggregate.getCreditOrderEntity();

        // 以用户作为锁粒度，串行化同一用户不同单号的并发调额，避免“查询→写入”间隙导致重复创建账户
        String lockKey = Constants.RedisKey.USER_CREDIT_ACCOUNT_LOCK + userId;
        RLock lock = redisService.getLock(lockKey);
        boolean locked = false;
        try {
            try {
                locked = lock.tryLock(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("调整账户积分额度，加锁被中断 userId:{}", userId, e);
                throw new AppException(ResponseCode.CREDIT_ACCOUNT_LOCK_TIMEOUT.getCode(), ResponseCode.CREDIT_ACCOUNT_LOCK_TIMEOUT.getInfo());
            }
            if (!locked) {
                log.warn("调整账户积分额度，加锁超时 userId:{}", userId);
                throw new AppException(ResponseCode.CREDIT_ACCOUNT_LOCK_TIMEOUT.getCode(), ResponseCode.CREDIT_ACCOUNT_LOCK_TIMEOUT.getInfo());
            }

            // 积分账户 - 增减值直接作为 total/available 的增量；新账户首次入账时双列一致
            UserCreditAccountPO userCreditAccountReq = new UserCreditAccountPO();
            userCreditAccountReq.setUserId(userId);
            userCreditAccountReq.setTotalAmount(creditAccountEntity.getAdjustAmount());
            userCreditAccountReq.setAvailableAmount(creditAccountEntity.getAdjustAmount());

            // 积分订单
            UserCreditOrderPO userCreditOrderReq = new UserCreditOrderPO();
            userCreditOrderReq.setUserId(creditOrderEntity.getUserId());
            userCreditOrderReq.setOrderId(creditOrderEntity.getOrderId());
            userCreditOrderReq.setTradeName(creditOrderEntity.getTradeName().getName());
            userCreditOrderReq.setTradeType(creditOrderEntity.getTradeType().getCode());
            userCreditOrderReq.setTradeAmount(creditOrderEntity.getTradeAmount());
            userCreditOrderReq.setOutBusinessNo(creditOrderEntity.getOutBusinessNo());

            // 以用户ID作为切分键，通过 doRouter 设定路由，保证事务内操作走同一个库表连接
            dbRouter.doRouter(userId);
            return transactionTemplate.execute(status -> {
                try {
                    // 1. 幂等：同一业务号已下单则直接返回原单号，避免重复增减账户
                    UserCreditOrderPO existOrder = userCreditOrderDao.queryUserCreditOrderByOutBusinessNo(userCreditOrderReq);
                    if (null != existOrder) {
                        log.info("调整账户积分额度，业务号已存在 userId:{} outBusinessNo:{} orderId:{}", userId, creditOrderEntity.getOutBusinessNo(), existOrder.getOrderId());
                        return existOrder.getOrderId();
                    }

                    // 2. 增减账户积分，账户不存在则新建（开启状态），存在则累加增减值
                    UserCreditAccountPO userCreditAccount = userCreditAccountDao.queryUserCreditAccountByUserId(userCreditAccountReq);
                    if (null == userCreditAccount) {
                        if (creditAccountEntity.getAdjustAmount().signum() < 0) {
                            throw new AppException(ResponseCode.ACCOUNT_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_QUOTA_ERROR.getInfo());
                        }
                        userCreditAccountReq.setAccountStatus(AccountStatusVO.open.getCode());
                        userCreditAccountDao.insert(userCreditAccountReq);
                    } else {
                        validateCreditAccount(userCreditAccount, creditAccountEntity.getAdjustAmount());
                        int updateCount = userCreditAccountDao.updateAddAmount(userCreditAccountReq);
                        if (1 != updateCount) {
                            throw new AppException(ResponseCode.ACCOUNT_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_QUOTA_ERROR.getInfo());
                        }
                    }

                    // 3. 保存积分订单
                    userCreditOrderDao.insert(userCreditOrderReq);
                    return creditOrderEntity.getOrderId();
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("调整账户积分额度，唯一索引冲突 userId:{} outBusinessNo:{}", userId, creditOrderEntity.getOutBusinessNo(), e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode());
                }
            });
        } finally {
            dbRouter.clear();
            if (locked) {
                lock.unlock();
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
