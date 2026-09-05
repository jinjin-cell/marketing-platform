package cn.qijiv.trigger.job;

import cn.qijiv.domain.activity.service.IRaffleActivityAccountQuotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;

/**
 * 更新活动订单超时过期定时任务
 * <p>
 * 将超过一个月「有效期内」仍未支付的订单批量置为过期，避免订单无限期停留在待支付状态。
 *
 * @author qijiv
 * @since 2026/09/05
 */
@Slf4j
@Component
public class UpdateActivityOrderExpiredJob {

    /** 活动订单额度服务 */
    @Resource
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;

    /**
     * 定时执行：将一个月以前未支付的订单批量置为过期
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void exec() {
        try {
            // 过期临界时间：当前时间往前推一个月
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            Date beforeTime = calendar.getTime();
            int expiredCount = raffleActivityAccountQuotaService.updateOrderExpired(beforeTime);
            log.info("定时任务，将超过一个月未支付的订单置为过期 beforeTime:{} expiredCount:{}", beforeTime, expiredCount);
        } catch (Exception e) {
            log.error("定时任务，将超过一个月未支付的订单置为过期失败", e);
        }
    }

}
