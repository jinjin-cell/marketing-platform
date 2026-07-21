package cn.qijiv.test.infrastructure;

import cn.qijiv.infrastructure.persistent.dao.IAwardDao;
import cn.qijiv.infrastructure.persistent.po.AwardPO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * 奖品 DAO 单元测试
 *
 * @author jinlujia
 * @since 2026-07-18
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AwardDaoTest {

    @Resource
    private IAwardDao awardDao;

    @Test
    public void test_queryAwardList() {
        List<AwardPO> list = awardDao.queryAwardList();
        log.info("查询结果：{}", list);
    }

}
