package cn.qijiv.trigger.job;

import cn.qijiv.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.qijiv.domain.activity.service.IRaffleActivitySkuStockService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 更新活动sku库存定时任务
 * 
 * @author qijiv
 * @since 2026/7/18
 */
@Slf4j
@Component()
public class UpdateActivitySkuStockJob {

    @Resource
    private IRaffleActivitySkuStockService skuStock;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 本地化任务注解；@Scheduled(cron = "0/5 * * * * ?")
     * 分布式任务注解；@XxlJob("SendMessageTaskJob")
     */
    @XxlJob("UpdateActivitySkuStockJob")
    public void exec() {
        // 为什么加锁？分布式应用N台机器部署互备，任务调度会有N个同时执行，那么这里需要增加抢占机制，谁抢占到谁就执行。完毕后，下一轮继续抢占。
        RLock lock = redissonClient.getLock("big-market-UpdateActivitySkuStockJob");
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(3, 0, TimeUnit.SECONDS);
            if (!isLocked) return;

            // 延迟队列趋势更新；队列为空时 poll 返回 null，结束本轮处理
            while (true) {
                ActivitySkuStockKeyVO activitySkuStockKeyVO = skuStock.takeQueueValue();
                if (null == activitySkuStockKeyVO) break;
                log.info("定时任务，更新活动sku库存 sku:{} activityId:{}", activitySkuStockKeyVO.getSku(), activitySkuStockKeyVO.getActivityId());
                skuStock.updateActivitySkuStock(activitySkuStockKeyVO.getSku());
            }
        } catch (InterruptedException e) {
            log.error("定时任务，更新活动sku库存被中断", e);
        } catch (Exception e) {
            log.error("定时任务，更新活动sku库存失败", e);
        } finally {
            if (isLocked) {
                lock.unlock();
            }
        }
    }

}
