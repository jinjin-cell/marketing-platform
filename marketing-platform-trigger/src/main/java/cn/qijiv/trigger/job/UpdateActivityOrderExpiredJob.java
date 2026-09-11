package cn.qijiv.trigger.job;

import cn.qijiv.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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
    @Resource
    private RedissonClient redissonClient;

    /**
     * 本地化任务注解；@Scheduled(cron = "0 0 2 * * ?")
     * 分布式任务注解；@XxlJob("UpdateActivityOrderExpiredJob")
     */
    @XxlJob("UpdateActivityOrderExpiredJob")
    public void exec() {
        // 为什么加锁？分布式应用N台机器部署互备，任务调度会有N个同时执行，那么这里需要增加抢占机制，谁抢占到谁就执行。完毕后，下一轮继续抢占。
        RLock lock = redissonClient.getLock("big-market-UpdateActivityOrderExpiredJob");
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(3, 0, TimeUnit.SECONDS);
            if (!isLocked) return;

            // 过期临界时间：当前时间往前推一个月
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            Date beforeTime = calendar.getTime();
            int expiredCount = raffleActivityAccountQuotaService.updateOrderExpired(beforeTime);
            log.info("定时任务，将超过一个月未支付的订单置为过期 beforeTime:{} expiredCount:{}", beforeTime, expiredCount);
        } catch (Exception e) {
            log.error("定时任务，将超过一个月未支付的订单置为过期失败", e);
        } finally {
            if (isLocked) {
                lock.unlock();
            }
        }
    }

}
