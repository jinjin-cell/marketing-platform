package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.dao.IRaffleActivityOrderDao;
import cn.qijiv.infrastructure.dao.po.RaffleActivityOrderPO;
import cn.qijiv.infrastructure.redis.OrderBloomFilterInitializer;
import cn.qijiv.infrastructure.redis.OrderBusinessNoBloomFilter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.concurrent.Executor;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 订单布隆过滤器初始化器单元测试：验证历史订单号加载逻辑。 */
@RunWith(MockitoJUnitRunner.class)
public class OrderBloomFilterInitializerTest {

    /** Mock 的订单 DAO，用于查询历史订单号。 */
    @Mock
    private IRaffleActivityOrderDao raffleActivityOrderDao;
    /** Mock 的订单号布隆过滤器，用于初始化与写入历史订单号。 */
    @Mock
    private OrderBusinessNoBloomFilter orderBloomFilter;
    /** Mock 的业务线程池，用于验证初始化任务异步提交。 */
    @Mock
    private Executor executor;

    /** 验证初始化器先初始化过滤器，再将所有历史订单号写入。 */
    @Test
    public void submitsInitializationAndLoadsEveryHistoricalBusinessKey() {
        RaffleActivityOrderPO first = order("user001", "business001");
        RaffleActivityOrderPO second = order("user002", "business002");
        when(raffleActivityOrderDao.queryAllBusinessKeys()).thenReturn(Arrays.asList(first, second));
        OrderBloomFilterInitializer initializer =
                new OrderBloomFilterInitializer(raffleActivityOrderDao, orderBloomFilter, executor);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));

        initializer.onApplicationEvent(mock(ApplicationReadyEvent.class));

        InOrder inOrder = inOrder(executor, orderBloomFilter, raffleActivityOrderDao);
        inOrder.verify(executor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));
        inOrder.verify(orderBloomFilter).initialize();
        inOrder.verify(raffleActivityOrderDao).queryAllBusinessKeys();
        inOrder.verify(orderBloomFilter).add("user001", "business001");
        inOrder.verify(orderBloomFilter).add("user002", "business002");
    }

    /** 构造指定用户与业务单号的订单 PO 对象。 */
    private RaffleActivityOrderPO order(String userId, String outBusinessNo) {
        RaffleActivityOrderPO order = new RaffleActivityOrderPO();
        order.setUserId(userId);
        order.setOutBusinessNo(outBusinessNo);
        return order;
    }
}
