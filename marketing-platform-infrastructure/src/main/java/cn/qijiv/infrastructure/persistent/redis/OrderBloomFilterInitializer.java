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

    private final IRaffleActivityOrderDao raffleActivityOrderDao;
    private final OrderBusinessNoBloomFilter orderBloomFilter;

    public OrderBloomFilterInitializer(IRaffleActivityOrderDao raffleActivityOrderDao,
                                       OrderBusinessNoBloomFilter orderBloomFilter) {
        this.raffleActivityOrderDao = raffleActivityOrderDao;
        this.orderBloomFilter = orderBloomFilter;
    }

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
