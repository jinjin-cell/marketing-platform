package cn.qijiv.trigger.http;

import cn.qijiv.trigger.job.SendMessageTaskJob;
import cn.qijiv.trigger.job.UpdateActivityOrderExpiredJob;
import cn.qijiv.trigger.job.UpdateActivitySkuStockJob;
import cn.qijiv.trigger.job.UpdateAwardStockJob;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 运维接口：手动触发 XXL-Job 定时任务，并列出可触发任务清单。
 *
 * <p>这些任务平时由 xxl-job-admin 调度，此接口仅用于本地/演示环境手工补偿。
 * <br>需要通过 {@code X-Admin-Token} 请求头校验，令牌取配置 {@code app.config.admin-token}。
 *
 * @author qijiv
 * @since 2026/09/14
 */
@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/admin/")
public class AdminController {

    /** 可触发的任务：key 为接口入参，value 为任务说明 */
    private static final Map<String, String> JOBS;

    static {
        Map<String, String> jobs = new LinkedHashMap<>();
        jobs.put("updateAwardStock", "更新奖品消耗库存（Redis 队列落库）");
        jobs.put("updateActivitySkuStock", "更新活动 SKU 库存（Redis 队列落库）");
        jobs.put("updateActivityOrderExpired", "将超过一个月的待支付订单置为过期");
        jobs.put("sendMessageTaskDb1", "补偿发送 ds1 库未发送的 MQ 任务");
        jobs.put("sendMessageTaskDb2", "补偿发送 ds2 库未发送的 MQ 任务");
        JOBS = Collections.unmodifiableMap(jobs);
    }

    /** 生产环境必须显式提供 ADMIN_TOKEN，不允许代码级默认令牌。 */
    @Value("${app.config.admin-token}")
    private String adminToken;

    @Resource
    private UpdateAwardStockJob updateAwardStockJob;
    @Resource
    private UpdateActivitySkuStockJob updateActivitySkuStockJob;
    @Resource
    private UpdateActivityOrderExpiredJob updateActivityOrderExpiredJob;
    @Resource
    private SendMessageTaskJob sendMessageTaskJob;

    /**
     * 列出可手动触发的任务
     *
     * <p>接口：{@code /api/v1/raffle/admin/query_job_list}
     */
    @RequestMapping(value = "query_job_list", method = RequestMethod.GET)
    public Response<Map<String, String>> queryJobList(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!isAuthorized(token)) {
            return Response.<Map<String, String>>builder()
                    .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                    .info("无权执行运维操作")
                    .build();
        }
        return Response.<Map<String, String>>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(JOBS)
                .build();
    }

    /**
     * 手动触发一次定时任务（同步执行，队列为空即结束）
     *
     * <p>接口只接受 POST，令牌通过 {@code X-Admin-Token} 请求头传递。
     */
    @RequestMapping(value = "run_job", method = RequestMethod.POST)
    public Response<List<String>> runJob(@RequestParam String job,
                                         @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        try {
            // 1. 令牌校验
            if (!isAuthorized(token)) {
                log.warn("运维任务触发被拒绝，令牌不正确 job:{}", job);
                return Response.<List<String>>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("令牌不正确，无法执行运维任务")
                        .build();
            }
            if (!JOBS.containsKey(job)) {
                log.warn("运维任务触发被拒绝，未知任务 job:{}", job);
                return Response.<List<String>>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("未知任务：" + job + "，可选：" + JOBS.keySet())
                        .build();
            }
            // 2. 同步执行；任务内部有 Redisson 抢占锁与队列空判断，重复触发是安全的
            log.info("运维任务触发开始 job:{}", job);
            List<String> logs = new ArrayList<>();
            logs.add("任务已执行：" + job + " - " + JOBS.get(job));
            switch (job) {
                case "updateAwardStock":
                    updateAwardStockJob.exec();
                    break;
                case "updateActivitySkuStock":
                    updateActivitySkuStockJob.exec();
                    break;
                case "updateActivityOrderExpired":
                    updateActivityOrderExpiredJob.exec();
                    break;
                case "sendMessageTaskDb1":
                    sendMessageTaskJob.exec_db01();
                    break;
                case "sendMessageTaskDb2":
                    sendMessageTaskJob.exec_db02();
                    break;
                default:
                    break;
            }
            log.info("运维任务触发完成 job:{}", job);
            return Response.<List<String>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(logs)
                    .build();
        } catch (Exception e) {
            log.error("运维任务触发失败 job:{}", job, e);
            return Response.<List<String>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    private boolean isAuthorized(String token) {
        return StringUtils.isNotBlank(token) && StringUtils.isNotBlank(adminToken)
                && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                adminToken.getBytes(StandardCharsets.UTF_8));
    }

}
