package cn.qijiv.test.trigger;

import cn.qijiv.trigger.api.IRaffleActivityService;
import cn.qijiv.trigger.api.dto.ActivityDrawRequestDTO;
import cn.qijiv.trigger.api.dto.ActivityDrawResponseDTO;
import cn.qijiv.types.model.Response;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 抽奖活动服务集成测试
 *
 * <p>验证活动装配与活动抽奖接口的完整链路（参与活动 -> 策略抽奖 -> 中奖落库）。</p>
 *
 * @author qijiv
 * @since 2026-08-19
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RaffleActivityControllerTest {

    /** 抽奖活动服务。 */
    @Resource
    private IRaffleActivityService raffleActivityService;

    /** 验证活动装配接口。 */
    @Test
    public void test_armory() {
        Response<Boolean> response = raffleActivityService.armory(100301L);
        log.info("测试结果：{}", JSON.toJSONString(response));
    }

    /** 验证活动抽奖接口。 */
    @Test
    public void test_draw() {
        ActivityDrawRequestDTO request = new ActivityDrawRequestDTO();
        request.setActivityId(100301L);
        request.setUserId("xiaofuge");
        Response<ActivityDrawResponseDTO> response = raffleActivityService.draw(request);
        log.info("请求参数：{}", JSON.toJSONString(request));
        log.info("测试结果：{}", JSON.toJSONString(response));
    }

}
