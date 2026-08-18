package cn.qijiv.infrastructure.persistent.redis;

import cn.qijiv.infrastructure.persistent.dao.IRaffleActivityOrderDao;
import cn.qijiv.infrastructure.persistent.po.RaffleActivityOrderPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.List;

/** 应用对外提供服务前，将全部历史订单业务键装入 Bloom Filter。 */
@Slf4j
@Component
public class OrderBloomFilterInitializer implements SmartInitializingSingleton {

    /** 活动订单 DAO，用于查询全量历史订单业务号 */
    private final IRaffleActivityOrderDao raffleActivityOrderDao;
    /** 订单业务号布隆过滤器 */
    private final OrderBusinessNoBloomFilter orderBloomFilter;

    public OrderBloomFilterInitializer(IRaffleActivityOrderDao raffleActivityOrderDao,
                                       OrderBusinessNoBloomFilter orderBloomFilter) {
        this.raffleActivityOrderDao = raffleActivityOrderDao;
        this.orderBloomFilter = orderBloomFilter;
    }

    /**
     * 应用单例初始化完成后，将全部历史订单业务号加载进布隆过滤器
     */
    @Override
    public void afterSingletonsInstantiated() {
        orderBloomFilter.initialize();
        List<RaffleActivityOrderPO> orders = raffleActivityOrderDao.queryAllBusinessKeys();
        for (RaffleActivityOrderPO order : orders) {
            orderBloomFilter.add(order.getUserId(), order.getOutBusinessNo());
        }
        log.info("订单业务号布隆过滤器初始化完成，装载历史订单数量: {}", orders.size());
    }
}
