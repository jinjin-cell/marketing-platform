package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.persistent.dao.IRaffleActivityOrderDao;
import cn.qijiv.infrastructure.persistent.po.RaffleActivityOrderPO;
import cn.qijiv.infrastructure.persistent.redis.OrderBloomFilterInitializer;
import cn.qijiv.infrastructure.persistent.redis.OrderBusinessNoBloomFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrderBloomFilterInitializerTest {

    @Mock
    private IRaffleActivityOrderDao raffleActivityOrderDao;
    @Mock
    private OrderBusinessNoBloomFilter orderBloomFilter;

    @Test
    public void initializesThenLoadsEveryHistoricalBusinessKey() {
        RaffleActivityOrderPO first = order("user001", "business001");
        RaffleActivityOrderPO second = order("user002", "business002");
        when(raffleActivityOrderDao.queryAllBusinessKeys()).thenReturn(Arrays.asList(first, second));
        OrderBloomFilterInitializer initializer =
                new OrderBloomFilterInitializer(raffleActivityOrderDao, orderBloomFilter);

        initializer.afterSingletonsInstantiated();

        InOrder inOrder = inOrder(orderBloomFilter, raffleActivityOrderDao);
        inOrder.verify(orderBloomFilter).initialize();
        inOrder.verify(raffleActivityOrderDao).queryAllBusinessKeys();
        inOrder.verify(orderBloomFilter).add("user001", "business001");
        inOrder.verify(orderBloomFilter).add("user002", "business002");
    }

    private RaffleActivityOrderPO order(String userId, String outBusinessNo) {
        RaffleActivityOrderPO order = new RaffleActivityOrderPO();
        order.setUserId(userId);
        order.setOutBusinessNo(outBusinessNo);
        return order;
    }
}
