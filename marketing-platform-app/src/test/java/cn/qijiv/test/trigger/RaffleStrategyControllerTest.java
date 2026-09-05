package cn.qijiv.test.trigger;

import cn.qijiv.trigger.api.IRaffleStrategyService;
import cn.qijiv.trigger.api.dto.RaffleAwardListRequestDTO;
import cn.qijiv.trigger.api.dto.RaffleAwardListResponseDTO;
import cn.qijiv.types.model.Response;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * 营销抽奖服务集成测试
 *
 * <p>验证按活动ID + 用户ID 查询奖品列表，并返回奖品解锁配置
 * （awardRuleLockCount / isAwardUnlock / waitUnlockCount）。</p>
 *
 * @author qijiv
 * @since 2026-08-19
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RaffleStrategyControllerTest {

    /** 抽奖策略服务。 */
    @Resource
    private IRaffleStrategyService raffleStrategyService;

    /** 验证按活动查询抽奖奖品列表（含解锁信息）。 */
    @Test
    public void test_queryRaffleAwardList() {
        RaffleAwardListRequestDTO request = new RaffleAwardListRequestDTO();
        request.setUserId("xiaofuge");
        request.setActivityId(100301L);
        Response<List<RaffleAwardListResponseDTO>> response =
                raffleStrategyService.queryRaffleAwardList(request);
        log.info("请求参数：{}", JSON.toJSONString(request));
        log.info("测试结果：{}", JSON.toJSONString(response));
    }

}
