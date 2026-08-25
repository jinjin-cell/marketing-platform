package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.persistent.dao.IRaffleActivityAccountDayDao;
import cn.qijiv.infrastructure.persistent.po.RaffleActivityAccountDayPO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.time.LocalDate;

/**
 * 活动日账户 DAO 单元测试
 *
 * <p>验证按用户、活动、日期查询日账户，该数据用于奖品解锁次数计算
 * （总次数 - 剩余次数 = 今日已参与抽奖次数）。</p>
 *
 * @author qijiv
 * @since 2026-08-19
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RaffleActivityAccountDayDaoTest {

    /** 活动日账户 DAO。 */
    @Resource
    private IRaffleActivityAccountDayDao raffleActivityAccountDayDao;

    /** 验证查询活动日账户并打印结果。 */
    @Test
    public void test_queryActivityAccountDayByUserId() {
        RaffleActivityAccountDayPO request = new RaffleActivityAccountDayPO();
        request.setActivityId(100301L);
        request.setUserId("xiaofuge");
        request.setDay(LocalDate.now().toString());
        RaffleActivityAccountDayPO day = raffleActivityAccountDayDao.queryActivityAccountDayByUserId(request);
        log.info("测试结果：{}", day);
    }

}
