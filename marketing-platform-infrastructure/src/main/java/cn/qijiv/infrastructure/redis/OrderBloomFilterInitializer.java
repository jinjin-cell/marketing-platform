package cn.qijiv.infrastructure.redis;

import cn.qijiv.infrastructure.dao.IRaffleActivityOrderDao;
import cn.qijiv.infrastructure.dao.po.RaffleActivityOrderPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executor;

/** 应用启动完成后，异步将全部历史订单业务键装入 Bloom Filter。 */
@Slf4j
@Component
public class OrderBloomFilterInitializer implements ApplicationListener<ApplicationReadyEvent> {

    /** 活动订单 DAO，用于查询全量历史订单业务号 */
    private final IRaffleActivityOrderDao raffleActivityOrderDao;
    /** 订单业务号布隆过滤器 */
    private final OrderBusinessNoBloomFilter orderBloomFilter;
    /** 应用统一业务线程池，避免阻塞 Spring 启动线程 */
    private final Executor executor;

    public OrderBloomFilterInitializer(IRaffleActivityOrderDao raffleActivityOrderDao,
                                       OrderBusinessNoBloomFilter orderBloomFilter,
                                       @Qualifier("threadPoolExecutor") Executor executor) {
        this.raffleActivityOrderDao = raffleActivityOrderDao;
        this.orderBloomFilter = orderBloomFilter;
        this.executor = executor;
    }

    /**
     * 应用完全就绪后提交初始化任务，不阻塞 Tomcat、Dubbo 的启动完成信号。
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        executor.execute(this::initialize);
        log.info("订单业务号布隆过滤器初始化任务已异步提交");
    }

    /** 加载历史订单业务号；异常只记录日志，不影响已启动的应用。 */
    private void initialize() {
        try {
            orderBloomFilter.initialize();
            List<RaffleActivityOrderPO> orders = raffleActivityOrderDao.queryAllBusinessKeys();
            for (RaffleActivityOrderPO order : orders) {
                orderBloomFilter.add(order.getUserId(), order.getOutBusinessNo());
            }
            log.info("订单业务号布隆过滤器初始化完成，装载历史订单数量: {}", orders.size());
        } catch (Exception e) {
            log.error("订单业务号布隆过滤器异步初始化失败", e);
        }
    }
}
