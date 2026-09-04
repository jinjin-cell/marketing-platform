package cn.qijiv.test.infrastructure;

import cn.qijiv.domain.credit.model.aggregate.TradeAggregate;
import cn.qijiv.domain.credit.model.entity.CreditAccountEntity;
import cn.qijiv.domain.credit.model.entity.CreditOrderEntity;
import cn.qijiv.domain.credit.model.valobj.TradeNameVO;
import cn.qijiv.domain.credit.model.valobj.TradeTypeVO;
import cn.qijiv.infrastructure.persistent.dao.IUserCreditAccountDao;
import cn.qijiv.infrastructure.persistent.dao.IUserCreditOrderDao;
import cn.qijiv.infrastructure.persistent.db.IDBRouterStrategy;
import cn.qijiv.infrastructure.persistent.po.UserCreditAccountPO;
import cn.qijiv.infrastructure.persistent.po.UserCreditOrderPO;
import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import cn.qijiv.infrastructure.persistent.repository.CreditRepository;
import cn.qijiv.types.common.Constants;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.api.RLock;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 积分调额仓储单元测试：验证按用户加锁、按业务号幂等与账户“查询→增减”逻辑。 */
@RunWith(MockitoJUnitRunner.class)
public class CreditRepositoryUnitTest {

    /** Mock 的 Redis 服务，用于模拟分布式锁。 */
    @Mock
    private IRedisService redisService;
    /** Mock 的积分账户 DAO。 */
    @Mock
    private IUserCreditAccountDao userCreditAccountDao;
    /** Mock 的积分订单 DAO。 */
    @Mock
    private IUserCreditOrderDao userCreditOrderDao;
    /** Mock 的分库路由策略。 */
    @Mock
    private IDBRouterStrategy dbRouter;
    /** Mock 的事务模板。 */
    @Mock
    private TransactionTemplate transactionTemplate;
    /** Mock 的事务状态。 */
    @Mock
    private TransactionStatus transactionStatus;
    /** Mock 的分布式锁。 */
    @Mock
    private RLock creditLock;

    /** 被测的积分调额仓储。 */
    @InjectMocks
    private CreditRepository creditRepository;

    /** 让事务模板直接执行回调，以便验证事务内行为。 */
    @Before
    public void executeTransactionCallbacks() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
    }

    /** 模拟分布式锁：获取成功。 */
    private void mockLockSuccess() {
        when(redisService.getLock(anyString())).thenReturn(creditLock);
        try {
            when(creditLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("模拟分布式锁获取被中断", e);
        }
    }

    /** 验证账户已存在时，只累加账户额度并插入订单，不重复建户。 */
    @Test
    public void saveTradeOrder_existingAccount_updatesQuotaAndInsertsOrder() {
        mockLockSuccess();
        TradeAggregate aggregate = createTradeAggregate();
        UserCreditAccountPO account = new UserCreditAccountPO();
        account.setAccountStatus("open");
        account.setAvailableAmount(new BigDecimal("100.00"));
        when(userCreditAccountDao.queryUserCreditAccountByUserId(any(UserCreditAccountPO.class))).thenReturn(account);
        when(userCreditAccountDao.updateAddAmount(any(UserCreditAccountPO.class))).thenReturn(1);

        String orderId = creditRepository.saveUserCreditTradeOrder(aggregate);

        assertEquals("123456789012", orderId);
        verify(dbRouter).doRouter("user001");
        verify(userCreditAccountDao).updateAddAmount(any(UserCreditAccountPO.class));
        verify(userCreditAccountDao, never()).insert(any(UserCreditAccountPO.class));

        ArgumentCaptor<UserCreditOrderPO> orderCaptor = ArgumentCaptor.forClass(UserCreditOrderPO.class);
        verify(userCreditOrderDao).insert(orderCaptor.capture());
        assertEquals("123456789012", orderCaptor.getValue().getOrderId());
        assertEquals("行为返利", orderCaptor.getValue().getTradeName());
        assertEquals("forward", orderCaptor.getValue().getTradeType());
        assertEquals(new BigDecimal("10.00"), orderCaptor.getValue().getTradeAmount());
        verify(dbRouter).clear();
        verify(creditLock).unlock();
    }

    /** 验证账户不存在时插入账户（状态 open），再插入订单。 */
    @Test
    public void saveTradeOrder_missingAccount_createsOpenAccountAndInsertsOrder() {
        mockLockSuccess();
        TradeAggregate aggregate = createTradeAggregate();
        when(userCreditAccountDao.queryUserCreditAccountByUserId(any(UserCreditAccountPO.class))).thenReturn(null);

        creditRepository.saveUserCreditTradeOrder(aggregate);

        ArgumentCaptor<UserCreditAccountPO> accountCaptor = ArgumentCaptor.forClass(UserCreditAccountPO.class);
        verify(userCreditAccountDao).insert(accountCaptor.capture());
        assertEquals("user001", accountCaptor.getValue().getUserId());
        assertEquals("open", accountCaptor.getValue().getAccountStatus());
        assertEquals(new BigDecimal("10.00"), accountCaptor.getValue().getTotalAmount());
        assertEquals(new BigDecimal("10.00"), accountCaptor.getValue().getAvailableAmount());
        verify(userCreditAccountDao, never()).updateAddAmount(any(UserCreditAccountPO.class));
        verify(userCreditOrderDao).insert(any(UserCreditOrderPO.class));
    }

    /** 验证业务号已存在时幂等返回原单号，不重复增减账户与落单。 */
    @Test
    public void saveTradeOrder_existBusinessNo_returnsExistOrderWithoutRepeatAdjust() {
        mockLockSuccess();
        TradeAggregate aggregate = createTradeAggregate();
        UserCreditOrderPO existOrder = new UserCreditOrderPO();
        existOrder.setOrderId("999999999999");
        when(userCreditOrderDao.queryUserCreditOrderByOutBusinessNo(any(UserCreditOrderPO.class))).thenReturn(existOrder);

        String orderId = creditRepository.saveUserCreditTradeOrder(aggregate);

        assertEquals("999999999999", orderId);
        verify(userCreditOrderDao, never()).insert(any(UserCreditOrderPO.class));
        verify(userCreditAccountDao, never()).insert(any(UserCreditAccountPO.class));
        verify(userCreditAccountDao, never()).updateAddAmount(any(UserCreditAccountPO.class));
        verify(creditLock).unlock();
    }

    /** 验证加锁超时时抛出积分账户加锁异常。 */
    @Test
    public void saveTradeOrder_lockTimeout_throwsCreditLockTimeout() {
        when(redisService.getLock(anyString())).thenReturn(creditLock);
        try {
            when(creditLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("模拟分布式锁获取被中断", e);
        }

        try {
            creditRepository.saveUserCreditTradeOrder(createTradeAggregate());
            fail("加锁超时应抛出积分账户加锁异常");
        } catch (AppException e) {
            assertEquals(ResponseCode.CREDIT_ACCOUNT_LOCK_TIMEOUT.getCode(), e.getCode());
        }
        verify(dbRouter, never()).doRouter("user001");
        verify(redisService).getLock(Constants.RedisKey.USER_CREDIT_ACCOUNT_LOCK + "user001");
    }

    /** 可用积分不足时，不得扣成负数，也不得生成无法兑现的积分订单。 */
    @Test
    public void saveTradeOrder_insufficientAvailableAmount_rejectsTradeWithoutCreatingOrder() {
        mockLockSuccess();
        TradeAggregate aggregate = createTradeAggregate(new BigDecimal("-10.00"));
        UserCreditAccountPO account = new UserCreditAccountPO();
        account.setAccountStatus("open");
        account.setAvailableAmount(new BigDecimal("5.00"));
        when(userCreditAccountDao.queryUserCreditAccountByUserId(any(UserCreditAccountPO.class))).thenReturn(account);

        try {
            creditRepository.saveUserCreditTradeOrder(aggregate);
            fail("可用积分不足应拒绝扣减");
        } catch (AppException e) {
            assertEquals(ResponseCode.ACCOUNT_QUOTA_ERROR.getCode(), e.getCode());
        }

        verify(userCreditOrderDao, never()).insert(any(UserCreditOrderPO.class));
        verify(userCreditAccountDao, never()).updateAddAmount(any(UserCreditAccountPO.class));
    }

    /** 冻结账户不得继续发生积分调额。 */
    @Test
    public void saveTradeOrder_closedAccount_rejectsTradeWithoutCreatingOrder() {
        mockLockSuccess();
        UserCreditAccountPO account = new UserCreditAccountPO();
        account.setAccountStatus("close");
        account.setAvailableAmount(new BigDecimal("100.00"));
        when(userCreditAccountDao.queryUserCreditAccountByUserId(any(UserCreditAccountPO.class))).thenReturn(account);

        try {
            creditRepository.saveUserCreditTradeOrder(createTradeAggregate());
            fail("冻结账户不得调额");
        } catch (AppException e) {
            assertEquals(ResponseCode.ILLEGAL_PARAMETER.getCode(), e.getCode());
        }

        verify(userCreditAccountDao, never()).updateAddAmount(any(UserCreditAccountPO.class));
        verify(userCreditOrderDao, never()).insert(any(UserCreditOrderPO.class));
    }

    /** 数据库条件更新未命中时，必须视为额度变更失败，避免继续创建订单。 */
    @Test
    public void saveTradeOrder_conditionalAccountUpdateMiss_rejectsTradeWithoutCreatingOrder() {
        mockLockSuccess();
        UserCreditAccountPO account = new UserCreditAccountPO();
        account.setAccountStatus("open");
        account.setAvailableAmount(new BigDecimal("5.00"));
        when(userCreditAccountDao.queryUserCreditAccountByUserId(any(UserCreditAccountPO.class))).thenReturn(account);
        when(userCreditAccountDao.updateAddAmount(any(UserCreditAccountPO.class))).thenReturn(0);

        try {
            creditRepository.saveUserCreditTradeOrder(createTradeAggregate(new BigDecimal("-3.00")));
            fail("条件更新未命中应拒绝交易");
        } catch (AppException e) {
            assertEquals(ResponseCode.ACCOUNT_QUOTA_ERROR.getCode(), e.getCode());
        }

        verify(userCreditOrderDao, never()).insert(any(UserCreditOrderPO.class));
    }

    /** 构造默认调额聚合：用户 user001 行为返利 +10。 */
    private TradeAggregate createTradeAggregate() {
        return createTradeAggregate(new BigDecimal("10.00"));
    }

    private TradeAggregate createTradeAggregate(BigDecimal amount) {
        CreditAccountEntity creditAccountEntity = CreditAccountEntity.builder()
                .userId("user001")
                .adjustAmount(amount)
                .build();
        CreditOrderEntity creditOrderEntity = CreditOrderEntity.builder()
                .userId("user001")
                .orderId("123456789012")
                .tradeName(TradeNameVO.REBATE)
                .tradeType(TradeTypeVO.FORWARD)
                .tradeAmount(amount)
                .outBusinessNo("business001")
                .build();
        return TradeAggregate.builder()
                .userId("user001")
                .creditAccountEntity(creditAccountEntity)
                .creditOrderEntity(creditOrderEntity)
                .build();
    }
}
