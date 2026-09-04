package cn.qijiv.infrastructure.persistent.repository;

import cn.qijiv.domain.activity.event.ActivitySkuStockZeroMessageEvent;
import cn.qijiv.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import cn.qijiv.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.qijiv.domain.activity.model.entity.*;
import cn.qijiv.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.qijiv.domain.activity.model.valobj.ActivityStateVO;
import cn.qijiv.domain.activity.model.valobj.OrderStateVO;
import cn.qijiv.domain.activity.model.valobj.UserRaffleOrderStateVO;
import cn.qijiv.domain.activity.repository.IActivityRepository;
import cn.qijiv.infrastructure.event.EventPublisher;
import cn.qijiv.infrastructure.persistent.dao.*;
import cn.qijiv.infrastructure.persistent.db.IDBRouterStrategy;
import cn.qijiv.infrastructure.persistent.po.*;
import cn.qijiv.infrastructure.persistent.redis.IRedisService;
import cn.qijiv.infrastructure.persistent.redis.OrderBusinessNoBloomFilter;
import cn.qijiv.types.common.Constants;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 抽奖活动仓储实现类
 *
 * @author qijiv
 * @since 2026/7/18
 */
@Slf4j
@Repository
public class ActivityRepository implements IActivityRepository {

    /** Redis 服务 */
    @Resource
    private IRedisService redisService;
    /** 抽奖活动 DAO */
    @Resource
    private IRaffleActivityDao raffleActivityDao;
    /** 活动商品 DAO */
    @Resource
    private IRaffleActivitySkuDao raffleActivitySkuDao;
    /** 活动次数 DAO */
    @Resource
    private IRaffleActivityCountDao raffleActivityCountDao;
    /** 活动订单 DAO */
    @Resource
    private IRaffleActivityOrderDao raffleActivityOrderDao;
    /** 活动账户 DAO */
    @Resource
    private IRaffleActivityAccountDao raffleActivityAccountDao;
    /** 活动账户 DAO */
    @Resource
    private IRaffleActivityAccountMonthDao raffleActivityAccountMonthDao;
    /** 活动账户 DAO */
    @Resource
    private IRaffleActivityAccountDayDao raffleActivityAccountDayDao;
    /** 用户抽奖订单 DAO */
    @Resource
    private IUserRaffleOrderDao userRaffleOrderDao;
    /** 编程式事务模板 */
    @Resource
    private TransactionTemplate transactionTemplate;
    /** 分库分表路由 */
    @Resource
    private IDBRouterStrategy dbRouter;
    /** 订单业务号布隆过滤器 */
    @Resource
    private OrderBusinessNoBloomFilter orderBloomFilter;
    /** 活动sku库存为0消息事件 */
    @Resource
    private ActivitySkuStockZeroMessageEvent activitySkuStockZeroMessageEvent;
    /** 事件发布器 */
    @Resource
    private EventPublisher eventPublisher;


    /**
     * 查询活动商品信息
     *
     * @param sku 商品sku
     * @return {@link ActivitySkuEntity}
     */
    @Override
    public ActivitySkuEntity queryActivitySku(Long sku) {
        RaffleActivitySkuPO raffleActivitySku = raffleActivitySkuDao.queryRaffleActivitySkuBySku(sku);
        return ActivitySkuEntity.builder()
                .sku(raffleActivitySku.getSku())
                .activityId(raffleActivitySku.getActivityId())
                .activityCountId(raffleActivitySku.getActivityCountId())
                .stockCount(raffleActivitySku.getStockCount())
                .stockCountSurplus(raffleActivitySku.getStockCountSurplus())
                .productAmount(raffleActivitySku.getProductAmount())
                .build();
    }

    /**
     * 查询抽奖活动信息
     *
     * @param activityId 活动id
     * @return {@link ActivityEntity}
     */
    @Override
    public ActivityEntity queryRaffleActivityByActivityId(Long activityId) {
        // 优先从缓存获取
        String cacheKey = Constants.RedisKey.ACTIVITY_KEY + activityId;
        ActivityEntity activityEntity = redisService.getValue(cacheKey);
        if (null != activityEntity) return activityEntity;
        // 从库中获取数据
        RaffleActivityPO raffleActivity = raffleActivityDao.queryRaffleActivityByActivityId(activityId);
        activityEntity = ActivityEntity.builder()
                .activityId(raffleActivity.getActivityId())
                .activityName(raffleActivity.getActivityName())
                .activityDesc(raffleActivity.getActivityDesc())
                .beginDateTime(raffleActivity.getBeginDateTime())
                .endDateTime(raffleActivity.getEndDateTime())
                .strategyId(raffleActivity.getStrategyId())
                .state(ActivityStateVO.valueOf(raffleActivity.getState()))
                .build();
        redisService.setValue(cacheKey, activityEntity);
        return activityEntity;
    }

    /**
     * 查询活动次数信息
     *
     * @param activityCountId 活动次数ID
     * @return {@link ActivityCountEntity}
     */
    @Override
    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
        // 优先从缓存获取
        String cacheKey = Constants.RedisKey.ACTIVITY_COUNT_KEY + activityCountId;
        ActivityCountEntity activityCountEntity = redisService.getValue(cacheKey);
        if (null != activityCountEntity) return activityCountEntity;
        // 从库中获取数据
        RaffleActivityCountPO raffleActivityCount = raffleActivityCountDao.queryRaffleActivityCountByActivityCountId(activityCountId);
        activityCountEntity = ActivityCountEntity.builder()
                .activityCountId(raffleActivityCount.getActivityCountId())
                .totalCount(raffleActivityCount.getTotalCount())
                .dayCount(raffleActivityCount.getDayCount())
                .monthCount(raffleActivityCount.getMonthCount())
                .build();
        redisService.setValue(cacheKey, activityCountEntity);
        return activityCountEntity;
    }

    /**
     * 根据外部业务号查询订单号
     *
     * @param userId        用户ID
     * @param outBusinessNo 外部业务号
     * @return 订单号，不存在时返回 null
     */
    @Override
    public ActivityOrderEntity queryActivityOrderByOutBusinessNo(String userId, String outBusinessNo) {
        if (!orderBloomFilter.mightContain(userId, outBusinessNo)) {
            return null;
        }

        try {
            dbRouter.doRouter(userId);
            RaffleActivityOrderPO order = raffleActivityOrderDao.queryByOutBusinessNo(userId, outBusinessNo);
            if (null == order) {
                return null;
            }
            OrderStateVO orderState = OrderStateVO.wait_pay.getCode().equals(order.getState())
                    ? OrderStateVO.wait_pay
                    : OrderStateVO.completed;
            return ActivityOrderEntity.builder()
                    .userId(order.getUserId())
                    .sku(order.getSku())
                    .activityId(order.getActivityId())
                    .activityName(order.getActivityName())
                    .strategyId(order.getStrategyId())
                    .orderId(order.getOrderId())
                    .orderTime(order.getOrderTime())
                    .totalCount(order.getTotalCount())
                    .dayCount(order.getDayCount())
                    .monthCount(order.getMonthCount())
                    .payAmount(order.getPayAmount())
                    .state(orderState)
                    .outBusinessNo(order.getOutBusinessNo())
                    .build();
        } finally {
            dbRouter.clear();
        }
    }

    /**
     * 保存充值额度订单并更新活动账户，事务内写订单与账户
     *
     * @param createQuotaOrderAggregate 创建充值额度订单聚合对象
     */
    @Override
    public void doSaveOrder(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        // 以 用户+活动 作为锁粒度，串行化同一用户同一活动的并发额度充值，避免“查询→更新”间隙导致重复创建账户或额度错乱
        String lockKey = Constants.RedisKey.ACTIVITY_ACCOUNT_LOCK
                + createQuotaOrderAggregate.getUserId()
                + Constants.UNDERLINE
                + createQuotaOrderAggregate.getActivityId();
        RLock lock = redisService.getLock(lockKey);
        boolean locked = false;
        try {
            // 至多等待 3 秒获取锁；拿不到说明同用户请求正在处理，直接失败让调用方重试，而不是并发写入
            try {
                locked = lock.tryLock(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("活动账户操作加锁被中断 userId: {} activityId: {}", createQuotaOrderAggregate.getUserId(), createQuotaOrderAggregate.getActivityId(), e);
                throw new AppException(ResponseCode.ACCOUNT_LOCK_TIMEOUT.getCode(), ResponseCode.ACCOUNT_LOCK_TIMEOUT.getInfo());
            }
            if (!locked) {
                log.warn("活动账户操作加锁超时 userId: {} activityId: {}", createQuotaOrderAggregate.getUserId(), createQuotaOrderAggregate.getActivityId());
                throw new AppException(ResponseCode.ACCOUNT_LOCK_TIMEOUT.getCode(), ResponseCode.ACCOUNT_LOCK_TIMEOUT.getInfo());
            }
            // 订单对象
            ActivityOrderEntity activityOrderEntity = createQuotaOrderAggregate.getActivityOrderEntity();
            RaffleActivityOrderPO raffleActivityOrder = new RaffleActivityOrderPO();
            raffleActivityOrder.setUserId(activityOrderEntity.getUserId());
            raffleActivityOrder.setSku(activityOrderEntity.getSku());
            raffleActivityOrder.setActivityId(activityOrderEntity.getActivityId());
            raffleActivityOrder.setActivityName(activityOrderEntity.getActivityName());
            raffleActivityOrder.setStrategyId(activityOrderEntity.getStrategyId());
            raffleActivityOrder.setOrderId(activityOrderEntity.getOrderId());
            raffleActivityOrder.setOrderTime(activityOrderEntity.getOrderTime());
            raffleActivityOrder.setTotalCount(createQuotaOrderAggregate.getTotalCount());
            raffleActivityOrder.setDayCount(createQuotaOrderAggregate.getDayCount());
            raffleActivityOrder.setMonthCount(createQuotaOrderAggregate.getMonthCount());
            raffleActivityOrder.setState(activityOrderEntity.getState().getCode());
            raffleActivityOrder.setOutBusinessNo(activityOrderEntity.getOutBusinessNo());

            // 账户对象
            RaffleActivityAccountPO raffleActivityAccount = new RaffleActivityAccountPO();
            raffleActivityAccount.setUserId(createQuotaOrderAggregate.getUserId());
            raffleActivityAccount.setActivityId(createQuotaOrderAggregate.getActivityId());
            raffleActivityAccount.setTotalCount(createQuotaOrderAggregate.getTotalCount());
            raffleActivityAccount.setTotalCountSurplus(createQuotaOrderAggregate.getTotalCount());
            raffleActivityAccount.setDayCount(createQuotaOrderAggregate.getDayCount());
            raffleActivityAccount.setDayCountSurplus(createQuotaOrderAggregate.getDayCount());
            raffleActivityAccount.setMonthCount(createQuotaOrderAggregate.getMonthCount());
            raffleActivityAccount.setMonthCountSurplus(createQuotaOrderAggregate.getMonthCount());

            // 事务前先写 Bloom，保证数据库中一旦存在订单，Bloom 中一定可能存在。
            // 后续数据库失败最多残留假阳性，查询时仍会回源数据库确认。
            orderBloomFilter.add(activityOrderEntity.getUserId(), activityOrderEntity.getOutBusinessNo());

            // 以用户ID作为切分键，通过 doRouter 设定路由【这样就保证了下面的操作，都是同一个链接下，也就保证了事务的特性】
            dbRouter.doRouter(createQuotaOrderAggregate.getUserId());
            // 编程式事务
            transactionTemplate.execute(status -> {
                try {
                    // 1. 写入订单
                    raffleActivityOrderDao.insert(raffleActivityOrder);
                    // 2. 更新账户 - 先查询账户是否存在，存在则累加额度，不存在则创建，避免“查询→写入”间隙导致两个请求同时插入引发唯一索引冲突
                    RaffleActivityAccountPO raffleActivityAccountRes = raffleActivityAccountDao.queryActivityAccountByUserId(raffleActivityAccount);
                    if (null == raffleActivityAccountRes) {
                        raffleActivityAccountDao.insert(raffleActivityAccount);
                    } else {
                        raffleActivityAccountDao.updateAccountQuota(raffleActivityAccount);
                    }
                    // 3. 当前月、日账户已存在时同步增加额度；不存在则在首次抽奖时按总账户镜像懒创建。
                    LocalDate today = LocalDate.now();
                    // 兼容仅装配总账户 DAO 的轻量测试/迁移场景；Spring 正式运行时两个 DAO 均会注入。
                    if (raffleActivityAccountMonthDao != null) {
                        raffleActivityAccountMonthDao.addAccountQuota(RaffleActivityAccountMonthPO.builder()
                                .userId(createQuotaOrderAggregate.getUserId())
                                .activityId(createQuotaOrderAggregate.getActivityId())
                                .month(today.toString().substring(0, 7))
                                .monthCount(createQuotaOrderAggregate.getMonthCount())
                                .monthCountSurplus(createQuotaOrderAggregate.getMonthCount())
                                .build());
                    }
                    if (raffleActivityAccountDayDao != null) {
                        raffleActivityAccountDayDao.addAccountQuota(RaffleActivityAccountDayPO.builder()
                                .userId(createQuotaOrderAggregate.getUserId())
                                .activityId(createQuotaOrderAggregate.getActivityId())
                                .day(today.toString())
                                .dayCount(createQuotaOrderAggregate.getDayCount())
                                .dayCountSurplus(createQuotaOrderAggregate.getDayCount())
                                .build());
                    }
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入订单记录，唯一索引冲突 userId: {} activityId: {} sku: {}", activityOrderEntity.getUserId(), activityOrderEntity.getActivityId(), activityOrderEntity.getSku(), e);
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

    /**
     * 缓存活动sku库存数量，已存在则不重复设置
     *
     * @param cacheKey   库存缓存键
     * @param stockCount 库存数量
     */
    @Override
    public void cacheActivitySkuStockCount(String cacheKey, Integer stockCount) {
        if (redisService.isExists(cacheKey)) return;
        redisService.setAtomicLong(cacheKey, stockCount);
    }

    /**
     * 扣减活动sku库存并加锁防超卖
     *
     * @param sku         商品sku
     * @param cacheKey    库存缓存键
     * @param endDateTime 活动结束时间
     * @return 扣减并加锁成功返回 true
     */
    @Override
    public boolean subtractionActivitySkuStock(Long sku, String cacheKey, Date endDateTime) {
        long surplus = redisService.decr(cacheKey);
        if (surplus == 0) {
            // 库存消耗没了以后，发送MQ消息，更新数据库库存
            eventPublisher.publish(activitySkuStockZeroMessageEvent.topic(), activitySkuStockZeroMessageEvent.buildEventMessage(sku));
        } else if (surplus < 0) {
            // 库存小于0，恢复为0个
            redisService.setAtomicLong(cacheKey, 0);
            return false;
        }

        // 1. 按照cacheKey decr 后的值，如 99、98、97 和 key 组成为库存锁的key进行使用。
        // 2. 加锁为了兜底，如果后续有恢复库存，手动处理等【运营是人来操作，会有这种情况发放，系统要做防护】，也不会超卖。因为所有的可用库存key，都被加锁了。
        // 3. 设置加锁时间为活动到期 + 延迟1天
        String lockKey = cacheKey + Constants.UNDERLINE + surplus;
        long expireMillis = endDateTime.getTime() - System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
        Boolean lock = redisService.setNx(lockKey, expireMillis, TimeUnit.MILLISECONDS);
        if (!lock) {
            log.info("活动sku库存加锁失败 {}", lockKey);
        }
        return lock;
    }

    /**
     * 发送活动sku库存消耗消息到延迟队列
     *
     * @param activitySkuStockKeyVO 活动sku库存键
     */
    @Override
    public void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO activitySkuStockKeyVO) {
        String cacheKey = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY;
        RBlockingQueue<ActivitySkuStockKeyVO> blockingQueue = redisService.getBlockingQueue(cacheKey);
        RDelayedQueue<ActivitySkuStockKeyVO> delayedQueue = redisService.getDelayedQueue(blockingQueue);
        delayedQueue.offer(activitySkuStockKeyVO, 3, TimeUnit.SECONDS);
    }

    /**
     * 从延迟队列取出活动sku库存消耗消息
     *
     * @return 活动sku库存键，队列为空时返回 null
     */
    @Override
    public ActivitySkuStockKeyVO takeQueueValue() {
        String cacheKey = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY;
        RBlockingQueue<ActivitySkuStockKeyVO> destinationQueue = redisService.getBlockingQueue(cacheKey);
        return destinationQueue.poll();
    }

    /**
     * 清空活动sku库存消耗队列
     */
    @Override
    public void clearQueueValue() {
        String cacheKey = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY;
        RBlockingQueue<ActivitySkuStockKeyVO> destinationQueue = redisService.getBlockingQueue(cacheKey);
        destinationQueue.clear();
    }

    /**
     * 更新活动sku库存，异步落库
     *
     * @param sku 商品sku
     */
    @Override
    public void updateActivitySkuStock(Long sku) {
        raffleActivitySkuDao.updateActivitySkuStock(sku);
    }

    /**
     * 清空活动sku库存
     *
     * @param sku 商品sku
     */
    @Override
    public void clearActivitySkuStock(Long sku) {
        raffleActivitySkuDao.clearActivitySkuStock(sku);
    }

    /**
     * 保存参与抽奖订单聚合对象，事务内扣减账户额度并写入参与订单
     *
     * @param createPartakeOrderAggregate 参与抽奖订单聚合对象
     */
    @Override
    public void saveCreatePartakeOrderAggregate(CreatePartakeOrderAggregate createPartakeOrderAggregate) {
        try {
            String userId = createPartakeOrderAggregate.getUserId();
            Long activityId = createPartakeOrderAggregate.getActivityId();
            ActivityAccountEntity activityAccountEntity = createPartakeOrderAggregate.getActivityAccountEntity();
            ActivityAccountMonthEntity activityAccountMonthEntity = createPartakeOrderAggregate.getActivityAccountMonthEntity();
            ActivityAccountDayEntity activityAccountDayEntity = createPartakeOrderAggregate.getActivityAccountDayEntity();
            UserRaffleOrderEntity userRaffleOrderEntity = createPartakeOrderAggregate.getUserRaffleOrderEntity();

            // 统一切换路由，以下事务内的所有操作，都走一个路由
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try {
                    // 1. 更新总账户
                    int totalCount = raffleActivityAccountDao.updateActivityAccountSubtractionQuota(
                            RaffleActivityAccountPO.builder()
                                    .userId(userId)
                                    .activityId(activityId)
                                    .build());
                    if (1 != totalCount) {
                        status.setRollbackOnly();
                        log.warn("写入创建参与活动记录，更新总账户额度不足，异常 userId: {} activityId: {}", userId, activityId);
                        throw new AppException(ResponseCode.ACCOUNT_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_QUOTA_ERROR.getInfo());
                    }

                    // 2. 创建或更新月账户，true - 存在则更新，false - 不存在则插入
                    if (createPartakeOrderAggregate.isExistAccountMonth()) {
                        int updateMonthCount = raffleActivityAccountMonthDao.updateActivityAccountMonthSubtractionQuota(
                                RaffleActivityAccountMonthPO.builder()
                                        .userId(userId)
                                        .activityId(activityId)
                                        .month(activityAccountMonthEntity.getMonth())
                                        .build());
                        if (1 != updateMonthCount) {
                            // 未更新成功则回滚
                            status.setRollbackOnly();
                            log.warn("写入创建参与活动记录，更新月账户额度不足，异常 userId: {} activityId: {} month: {}", userId, activityId, activityAccountMonthEntity.getMonth());
                            throw new AppException(ResponseCode.ACCOUNT_MONTH_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_MONTH_QUOTA_ERROR.getInfo());
                        }
                    } else {
                        raffleActivityAccountMonthDao.insertActivityAccountMonth(RaffleActivityAccountMonthPO.builder()
                                .userId(activityAccountMonthEntity.getUserId())
                                .activityId(activityAccountMonthEntity.getActivityId())
                                .month(activityAccountMonthEntity.getMonth())
                                .monthCount(activityAccountMonthEntity.getMonthCount())
                                .monthCountSurplus(activityAccountMonthEntity.getMonthCountSurplus() - 1)
                                .build());
                        // 新创建月账户，则更新总账表中月镜像额度
                        raffleActivityAccountDao.updateActivityAccountMonthSurplusImageQuota(RaffleActivityAccountPO.builder()
                                .userId(userId)
                                .activityId(activityId)
                                .monthCountSurplus(activityAccountEntity.getMonthCountSurplus())
                                .build());
                    }

                    // 3. 创建或更新日账户，true - 存在则更新，false - 不存在则插入
                    if (createPartakeOrderAggregate.isExistAccountDay()) {
                        int updateDayCount = raffleActivityAccountDayDao.updateActivityAccountDaySubtractionQuota(RaffleActivityAccountDayPO.builder()
                                .userId(userId)
                                .activityId(activityId)
                                .day(activityAccountDayEntity.getDay())
                                .build());
                        if (1 != updateDayCount) {
                            // 未更新成功则回滚
                            status.setRollbackOnly();
                            log.warn("写入创建参与活动记录，更新日账户额度不足，异常 userId: {} activityId: {} day: {}", userId, activityId, activityAccountDayEntity.getDay());
                            throw new AppException(ResponseCode.ACCOUNT_DAY_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_DAY_QUOTA_ERROR.getInfo());
                        }
                    } else {
                        raffleActivityAccountDayDao.insertActivityAccountDay(RaffleActivityAccountDayPO.builder()
                                .userId(activityAccountDayEntity.getUserId())
                                .activityId(activityAccountDayEntity.getActivityId())
                                .day(activityAccountDayEntity.getDay())
                                .dayCount(activityAccountDayEntity.getDayCount())
                                .dayCountSurplus(activityAccountDayEntity.getDayCountSurplus() - 1)
                                .build());
                        // 新创建日账户，则更新总账表中日镜像额度
                        raffleActivityAccountDao.updateActivityAccountDaySurplusImageQuota(RaffleActivityAccountPO.builder()
                                .userId(userId)
                                .activityId(activityId)
                                .dayCountSurplus(activityAccountEntity.getDayCountSurplus())
                                .build());
                    }

                    // 4. 写入参与活动订单
                    userRaffleOrderDao.insert(UserRaffleOrderPO.builder()
                            .userId(userRaffleOrderEntity.getUserId())
                            .activityId(userRaffleOrderEntity.getActivityId())
                            .activityName(userRaffleOrderEntity.getActivityName())
                            .strategyId(userRaffleOrderEntity.getStrategyId())
                            .orderId(userRaffleOrderEntity.getOrderId())
                            .orderTime(userRaffleOrderEntity.getOrderTime())
                            .orderState(userRaffleOrderEntity.getOrderState().getCode())
                            .build());

                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入创建参与活动记录，唯一索引冲突 userId: {} activityId: {}", userId, activityId, e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
        } finally {
            dbRouter.clear();
        }
    }


    /**
     * 查询账户日账户
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @param day        日期
     * @return 日账户实体，不存在时返回 null
     */
    @Override
    public ActivityAccountDayEntity queryActivityAccountDayByUserId(String userId, Long activityId, String day) {
        // 1. 查询账户
        RaffleActivityAccountDayPO raffleActivityAccountDayReq = new RaffleActivityAccountDayPO();
        raffleActivityAccountDayReq.setUserId(userId);
        raffleActivityAccountDayReq.setActivityId(activityId);
        raffleActivityAccountDayReq.setDay(day);
        RaffleActivityAccountDayPO raffleActivityAccountDayRes = raffleActivityAccountDayDao.queryActivityAccountDayByUserId(raffleActivityAccountDayReq);
        if (null == raffleActivityAccountDayRes) return null;
        // 2. 转换对象
        return ActivityAccountDayEntity.builder()
                .userId(raffleActivityAccountDayRes.getUserId())
                .activityId(raffleActivityAccountDayRes.getActivityId())
                .day(raffleActivityAccountDayRes.getDay())
                .dayCount(raffleActivityAccountDayRes.getDayCount())
                .dayCountSurplus(raffleActivityAccountDayRes.getDayCountSurplus())
                .build();

    }

    /**
     * 查询账户
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @return 账户实体，不存在时返回 null
     */
    @Override
    public ActivityAccountEntity queryActivityAccountByUserId(String userId, Long activityId) {
        // 1. 查询账户
        RaffleActivityAccountPO raffleActivityAccountReq = new RaffleActivityAccountPO();
        raffleActivityAccountReq.setUserId(userId);
        raffleActivityAccountReq.setActivityId(activityId);
        RaffleActivityAccountPO raffleActivityAccountRes = raffleActivityAccountDao.queryActivityAccountByUserId(raffleActivityAccountReq);
        if (null == raffleActivityAccountRes) return null;
        // 2. 转换对象
        return ActivityAccountEntity.builder()
                .userId(raffleActivityAccountRes.getUserId())
                .activityId(raffleActivityAccountRes.getActivityId())
                .totalCount(raffleActivityAccountRes.getTotalCount())
                .totalCountSurplus(raffleActivityAccountRes.getTotalCountSurplus())
                .dayCount(raffleActivityAccountRes.getDayCount())
                .dayCountSurplus(raffleActivityAccountRes.getDayCountSurplus())
                .monthCount(raffleActivityAccountRes.getMonthCount())
                .monthCountSurplus(raffleActivityAccountRes.getMonthCountSurplus())
                .build();

    }

    /**
     * 账户月账户
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @param month      月份
     * @return 月账户实体，不存在时返回 null
     */
    @Override
    public ActivityAccountMonthEntity queryActivityAccountMonthByUserId(String userId, Long activityId, String month) {
        // 1. 查询账户
        RaffleActivityAccountMonthPO raffleActivityAccountMonthReq = new RaffleActivityAccountMonthPO();
        raffleActivityAccountMonthReq.setUserId(userId);
        raffleActivityAccountMonthReq.setActivityId(activityId);
        raffleActivityAccountMonthReq.setMonth(month);
        RaffleActivityAccountMonthPO raffleActivityAccountMonthRes = raffleActivityAccountMonthDao.queryActivityAccountMonthByUserId(raffleActivityAccountMonthReq);
        if (null == raffleActivityAccountMonthRes) return null;
        // 2. 转换对象
        return ActivityAccountMonthEntity.builder()
                .userId(raffleActivityAccountMonthRes.getUserId())
                .activityId(raffleActivityAccountMonthRes.getActivityId())
                .month(raffleActivityAccountMonthRes.getMonth())
                .monthCount(raffleActivityAccountMonthRes.getMonthCount())
                .monthCountSurplus(raffleActivityAccountMonthRes.getMonthCountSurplus())
                .build();

    }

    /**
     * 查询用户参与活动订单
     *
     * @param partakeRaffleActivityEntity 参与活动实体
     * @return 用户抽奖订单实体，不存在时返回 null
     */
    @Override
    public UserRaffleOrderEntity queryNoUsedRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity) {
        // 查询数据
        UserRaffleOrderPO userRaffleOrderReq = new UserRaffleOrderPO();
        userRaffleOrderReq.setUserId(partakeRaffleActivityEntity.getUserId());
        userRaffleOrderReq.setActivityId(partakeRaffleActivityEntity.getActivityId());
        UserRaffleOrderPO userRaffleOrderRes = userRaffleOrderDao.queryNoUsedRaffleOrder(userRaffleOrderReq);
        if (null == userRaffleOrderRes) return null;
        // 封装结果
        UserRaffleOrderEntity userRaffleOrderEntity = new UserRaffleOrderEntity();
        userRaffleOrderEntity.setUserId(userRaffleOrderRes.getUserId());
        userRaffleOrderEntity.setActivityId(userRaffleOrderRes.getActivityId());
        userRaffleOrderEntity.setActivityName(userRaffleOrderRes.getActivityName());
        userRaffleOrderEntity.setStrategyId(userRaffleOrderRes.getStrategyId());
        userRaffleOrderEntity.setOrderId(userRaffleOrderRes.getOrderId());
        userRaffleOrderEntity.setOrderTime(userRaffleOrderRes.getOrderTime());
        userRaffleOrderEntity.setOrderState(UserRaffleOrderStateVO.valueOf(userRaffleOrderRes.getOrderState()));
        return userRaffleOrderEntity;

    }

    /**
     * 查询活动sku列表
     *
     * @param activityId 活动ID
     * @return 活动SKU实体列表
     */
    @Override
    public List<ActivitySkuEntity> queryActivitySkuListByActivityId(Long activityId) {
        List<RaffleActivitySkuPO> raffleActivitySkus = raffleActivitySkuDao.queryActivitySkuListByActivityId(activityId);
        List<ActivitySkuEntity> activitySkuEntities = new ArrayList<>(raffleActivitySkus.size());
        for (RaffleActivitySkuPO raffleActivitySku:raffleActivitySkus){
            ActivitySkuEntity activitySkuEntity = new ActivitySkuEntity();
            activitySkuEntity.setSku(raffleActivitySku.getSku());
            activitySkuEntity.setActivityCountId(raffleActivitySku.getActivityCountId());
            activitySkuEntity.setStockCount(raffleActivitySku.getStockCount());
            activitySkuEntity.setStockCountSurplus(raffleActivitySku.getStockCountSurplus());
            activitySkuEntity.setProductAmount(raffleActivitySku.getProductAmount());
            activitySkuEntities.add(activitySkuEntity);
        }
        return activitySkuEntities;
    }

    /**
     * 查询活动今日参与次数
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 活动今日参与次数
     */
    @Override
    public Integer queryRaffleActivityAccountDayPartakeCount(Long activityId, String userId) {
        // 查询今日账户
        ActivityAccountDayEntity activityAccountDayEntity = queryActivityAccountDayByUserId(userId, activityId, LocalDate.now().toString());
        if (null == activityAccountDayEntity) return 0;
        // 总次数 - 剩余次数 = 今日已参与次数
        return activityAccountDayEntity.getDayCount() - activityAccountDayEntity.getDayCountSurplus();
    }

    @Override
    public ActivityAccountEntity queryActivityAccountEntity(Long activityId, String userId) {
        return queryActivityAccountByUserId(userId, activityId);
    }

    @Override
    public Integer queryRaffleActivityAccountPartakeCount(Long activityId, String userId) {
        ActivityAccountEntity account = queryActivityAccountByUserId(userId, activityId);
        if (account == null || account.getTotalCount() == null || account.getTotalCountSurplus() == null) {
            return 0;
        }
        // 权重规则比较的是活动维度累计使用次数，而不是当天次数。
        return Math.max(0, account.getTotalCount() - account.getTotalCountSurplus());
    }

    /**
     * 保存订单
     *
     * @param createOrderAggregate 创建订单聚合对象
     */
    @Override
    public void doSaveNoPayOrder(CreateQuotaOrderAggregate createOrderAggregate) {
        RLock lock = redisService.getLock(Constants.RedisKey.ACTIVITY_ACCOUNT_LOCK + createOrderAggregate.getUserId() + Constants.UNDERLINE + createOrderAggregate.getActivityId());
        try {
            lock.lock(3, TimeUnit.SECONDS);
            // 订单对象
            ActivityOrderEntity activityOrderEntity = createOrderAggregate.getActivityOrderEntity();
            RaffleActivityOrderPO raffleActivityOrder = new RaffleActivityOrderPO();
            raffleActivityOrder.setUserId(activityOrderEntity.getUserId());
            raffleActivityOrder.setSku(activityOrderEntity.getSku());
            raffleActivityOrder.setActivityId(activityOrderEntity.getActivityId());
            raffleActivityOrder.setActivityName(activityOrderEntity.getActivityName());
            raffleActivityOrder.setStrategyId(activityOrderEntity.getStrategyId());
            raffleActivityOrder.setOrderId(activityOrderEntity.getOrderId());
            raffleActivityOrder.setOrderTime(activityOrderEntity.getOrderTime());
            raffleActivityOrder.setTotalCount(activityOrderEntity.getTotalCount());
            raffleActivityOrder.setDayCount(activityOrderEntity.getDayCount());
            raffleActivityOrder.setMonthCount(activityOrderEntity.getMonthCount());
            raffleActivityOrder.setTotalCount(createOrderAggregate.getTotalCount());
            raffleActivityOrder.setDayCount(createOrderAggregate.getDayCount());
            raffleActivityOrder.setMonthCount(createOrderAggregate.getMonthCount());
            raffleActivityOrder.setPayAmount(activityOrderEntity.getPayAmount());
            raffleActivityOrder.setState(activityOrderEntity.getState().getCode());
            raffleActivityOrder.setOutBusinessNo(activityOrderEntity.getOutBusinessNo());

            // 账户对象 - 总
            RaffleActivityAccountPO raffleActivityAccount = new RaffleActivityAccountPO();
            raffleActivityAccount.setUserId(createOrderAggregate.getUserId());
            raffleActivityAccount.setActivityId(createOrderAggregate.getActivityId());
            raffleActivityAccount.setTotalCount(createOrderAggregate.getTotalCount());
            raffleActivityAccount.setTotalCountSurplus(createOrderAggregate.getTotalCount());
            raffleActivityAccount.setDayCount(createOrderAggregate.getDayCount());
            raffleActivityAccount.setDayCountSurplus(createOrderAggregate.getDayCount());
            raffleActivityAccount.setMonthCount(createOrderAggregate.getMonthCount());
            raffleActivityAccount.setMonthCountSurplus(createOrderAggregate.getMonthCount());

            // 账户对象 - 月
            RaffleActivityAccountMonthPO raffleActivityAccountMonth = new RaffleActivityAccountMonthPO();
            raffleActivityAccountMonth.setUserId(createOrderAggregate.getUserId());
            raffleActivityAccountMonth.setActivityId(createOrderAggregate.getActivityId());
            raffleActivityAccountMonth.setMonth(RaffleActivityAccountMonthPO.currentMonth());
            raffleActivityAccountMonth.setMonthCount(createOrderAggregate.getMonthCount());
            raffleActivityAccountMonth.setMonthCountSurplus(createOrderAggregate.getMonthCount());

            // 账户对象 - 日
            RaffleActivityAccountDayPO raffleActivityAccountDay = new RaffleActivityAccountDayPO();
            raffleActivityAccountDay.setUserId(createOrderAggregate.getUserId());
            raffleActivityAccountDay.setActivityId(createOrderAggregate.getActivityId());
            raffleActivityAccountDay.setDay(RaffleActivityAccountDayPO.currentDay());
            raffleActivityAccountDay.setDayCount(createOrderAggregate.getDayCount());
            raffleActivityAccountDay.setDayCountSurplus(createOrderAggregate.getDayCount());

            // 以用户ID作为切分键，通过 doRouter 设定路由【这样就保证了下面的操作，都是同一个链接下，也就保证了事务的特性】
            dbRouter.doRouter(createOrderAggregate.getUserId());
            // 编程式事务
            transactionTemplate.execute(status -> {
                try {
                    // 1. 写入订单
                    raffleActivityOrderDao.insert(raffleActivityOrder);
                    // 2. 更新账户 - 总
                    RaffleActivityAccountPO raffleActivityAccountRes = raffleActivityAccountDao.queryAccountByUserId(raffleActivityAccount.getUserId(), raffleActivityAccount.getActivityId());
                    if (null == raffleActivityAccountRes) {
                        raffleActivityAccountDao.insert(raffleActivityAccount);
                    } else {
                        raffleActivityAccountDao.updateAccountQuota(raffleActivityAccount);
                    }
                    // 4. 更新账户 - 月
                    raffleActivityAccountMonthDao.addAccountQuota(raffleActivityAccountMonth);
                    // 5. 更新账户 - 日
                    raffleActivityAccountDayDao.addAccountQuota(raffleActivityAccountDay);
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入订单记录，唯一索引冲突 userId: {} activityId: {} sku: {}", activityOrderEntity.getUserId(), activityOrderEntity.getActivityId(), activityOrderEntity.getSku(), e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
            orderBloomFilter.add(activityOrderEntity.getUserId(), activityOrderEntity.getOutBusinessNo());
        } finally {
            dbRouter.clear();
            lock.unlock();
        }
    }

    /**
     * 保存订单
     *
     * @param createOrderAggregate 创建订单聚合对象
     */
    @Override
    public void doSaveCreditPayOrder(CreateQuotaOrderAggregate createOrderAggregate) {
        try {
            // 创建交易订单
            ActivityOrderEntity activityOrderEntity = createOrderAggregate.getActivityOrderEntity();
            RaffleActivityOrderPO raffleActivityOrder = new RaffleActivityOrderPO();
            raffleActivityOrder.setUserId(activityOrderEntity.getUserId());
            raffleActivityOrder.setSku(activityOrderEntity.getSku());
            raffleActivityOrder.setActivityId(activityOrderEntity.getActivityId());
            raffleActivityOrder.setActivityName(activityOrderEntity.getActivityName());
            raffleActivityOrder.setStrategyId(activityOrderEntity.getStrategyId());
            raffleActivityOrder.setOrderId(activityOrderEntity.getOrderId());
            raffleActivityOrder.setOrderTime(activityOrderEntity.getOrderTime());
            raffleActivityOrder.setTotalCount(activityOrderEntity.getTotalCount());
            raffleActivityOrder.setDayCount(activityOrderEntity.getDayCount());
            raffleActivityOrder.setMonthCount(activityOrderEntity.getMonthCount());
            raffleActivityOrder.setTotalCount(createOrderAggregate.getTotalCount());
            raffleActivityOrder.setDayCount(createOrderAggregate.getDayCount());
            raffleActivityOrder.setMonthCount(createOrderAggregate.getMonthCount());
            raffleActivityOrder.setPayAmount(activityOrderEntity.getPayAmount());
            raffleActivityOrder.setState(activityOrderEntity.getState().getCode());
            raffleActivityOrder.setOutBusinessNo(activityOrderEntity.getOutBusinessNo());

            // 以用户ID作为切分键，通过 doRouter 设定路由【这样就保证了下面的操作，都是同一个链接下，也就保证了事务的特性】
            dbRouter.doRouter(createOrderAggregate.getUserId());

            // 编程式事务
            transactionTemplate.execute(status -> {
                try {
                    raffleActivityOrderDao.insert(raffleActivityOrder);
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入订单记录，唯一索引冲突 userId: {} activityId: {} sku: {}", activityOrderEntity.getUserId(), activityOrderEntity.getActivityId(), activityOrderEntity.getSku(), e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
            orderBloomFilter.add(activityOrderEntity.getUserId(), activityOrderEntity.getOutBusinessNo());
        } finally {
            dbRouter.clear();
        }
    }

    /**
     * 更新订单
     *
     * @param deliveryOrderEntity 订单实体
     */
    @Override
    public void updateOrder(DeliveryOrderEntity deliveryOrderEntity) {
        RLock lock = redisService.getLock(Constants.RedisKey.ACTIVITY_ACCOUNT_UPDATE_LOCK + deliveryOrderEntity.getUserId());
        try {
            lock.lock(3, TimeUnit.SECONDS);

            // 查询订单
            RaffleActivityOrderPO raffleActivityOrderReq = new RaffleActivityOrderPO();
            raffleActivityOrderReq.setUserId(deliveryOrderEntity.getUserId());
            raffleActivityOrderReq.setOutBusinessNo(deliveryOrderEntity.getOutBusinessNo());
            RaffleActivityOrderPO raffleActivityOrderRes = raffleActivityOrderDao.queryRaffleActivityOrder(raffleActivityOrderReq);

            // 账户对象 - 总
            RaffleActivityAccountPO raffleActivityAccount = new RaffleActivityAccountPO();
            raffleActivityAccount.setUserId(raffleActivityOrderRes.getUserId());
            raffleActivityAccount.setActivityId(raffleActivityOrderRes.getActivityId());
            raffleActivityAccount.setTotalCount(raffleActivityOrderRes.getTotalCount());
            raffleActivityAccount.setTotalCountSurplus(raffleActivityOrderRes.getTotalCount());
            raffleActivityAccount.setDayCount(raffleActivityOrderRes.getDayCount());
            raffleActivityAccount.setDayCountSurplus(raffleActivityOrderRes.getDayCount());
            raffleActivityAccount.setMonthCount(raffleActivityOrderRes.getMonthCount());
            raffleActivityAccount.setMonthCountSurplus(raffleActivityOrderRes.getMonthCount());

            // 账户对象 - 月
            RaffleActivityAccountMonthPO raffleActivityAccountMonth = new RaffleActivityAccountMonthPO();
            raffleActivityAccountMonth.setUserId(raffleActivityOrderRes.getUserId());
            raffleActivityAccountMonth.setActivityId(raffleActivityOrderRes.getActivityId());
            raffleActivityAccountMonth.setMonth(RaffleActivityAccountMonthPO.currentMonth());
            raffleActivityAccountMonth.setMonthCount(raffleActivityOrderRes.getMonthCount());
            raffleActivityAccountMonth.setMonthCountSurplus(raffleActivityOrderRes.getMonthCount());

            // 账户对象 - 日
            RaffleActivityAccountDayPO raffleActivityAccountDay = new RaffleActivityAccountDayPO();
            raffleActivityAccountDay.setUserId(raffleActivityOrderRes.getUserId());
            raffleActivityAccountDay.setActivityId(raffleActivityOrderRes.getActivityId());
            raffleActivityAccountDay.setDay(RaffleActivityAccountDayPO.currentDay());
            raffleActivityAccountDay.setDayCount(raffleActivityOrderRes.getDayCount());
            raffleActivityAccountDay.setDayCountSurplus(raffleActivityOrderRes.getDayCount());


            dbRouter.doRouter(deliveryOrderEntity.getUserId());
            // 编程式事务
            transactionTemplate.execute(status -> {
                try {
                    // 1. 更新订单
                    int updateCount = raffleActivityOrderDao.updateOrderCompleted(raffleActivityOrderReq);
                    if (1 != updateCount) {
                        status.setRollbackOnly();
                        return 1;
                    }
                    // 2. 更新账户 - 总
                    RaffleActivityAccountPO raffleActivityAccountRes = raffleActivityAccountDao.queryAccountByUserId(raffleActivityAccount.getUserId(), raffleActivityAccount.getActivityId());
                    if (null == raffleActivityAccountRes) {
                        raffleActivityAccountDao.insert(raffleActivityAccount);
                    } else {
                        raffleActivityAccountDao.updateAccountQuota(raffleActivityAccount);
                    }
                    // 4. 更新账户 - 月
                    raffleActivityAccountMonthDao.addAccountQuota(raffleActivityAccountMonth);
                    // 5. 更新账户 - 日
                    raffleActivityAccountDayDao.addAccountQuota(raffleActivityAccountDay);
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("更新订单记录，完成态，唯一索引冲突 userId: {} outBusinessNo: {}", deliveryOrderEntity.getUserId(), deliveryOrderEntity.getOutBusinessNo(), e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
        } finally {
            dbRouter.clear();
            lock.unlock();
        }
    }



}
