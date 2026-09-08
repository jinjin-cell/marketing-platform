package cn.qijiv.aop;

import cn.qijiv.types.annotations.DCCValue;
import cn.qijiv.types.annotations.SentinelGuard;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.model.Response;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Sentinel 熔断降级切面（最外层，先于 Redis 每用户限流）。
 *
 * <p>拦截顺序：DCC 人工降级(0004) → Sentinel 流控/熔断(0006) → Redis 每用户限流(0005) → 业务。
 *
 * <ul>
 *   <li>degradeSwitch 非 open 时，直接返回「活动已降级」，不消耗 Sentinel 资源；</li>
 *   <li>命中 Sentinel 流控/熔断规则（BlockException）时，返回「访问熔断拦截」；</li>
 *   <li>业务异常仍交由 controller 统一处理；意外异常会通过 controller 的 Tracer.trace 计入熔断统计。</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
@Order(-100)
public class SentinelGuardAOP {

    /** DCC 动态降级开关，默认 open；由 Zookeeper 配置中心下发 */
    @DCCValue("degradeSwitch:open")
    private String degradeSwitch;

    @Pointcut("@annotation(sentinelGuard)")
    public void sentinelPoint(SentinelGuard sentinelGuard) {
    }

    @Around("sentinelPoint(sentinelGuard)")
    public Object doGuard(ProceedingJoinPoint jp, SentinelGuard sentinelGuard) throws Throwable {
        // 0. 人工降级总闸：close 直接返回，不进入 Sentinel
        if (!"open".equals(degradeSwitch)) {
            log.info("活动已降级，跳过 Sentinel，直接返回降级响应");
            return buildResponse(ResponseCode.DEGRADE_SWITCH);
        }

        String resource = StringUtils.isNotBlank(sentinelGuard.value())
                ? sentinelGuard.value()
                : jp.getSignature().toLongString();
        Entry entry = null;
        try {
            // 1. 进入 Sentinel 资源：命中流控/熔断规则抛 BlockException
            entry = SphU.entry(resource);
            return jp.proceed();
        } catch (BlockException e) {
            // 2. 流控/熔断拦截
            log.warn("Sentinel 拦截 resource:{} rule:{}", resource, e.getRule());
            return buildResponse(ResponseCode.HYSTRIX);
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    private Response<Object> buildResponse(ResponseCode code) {
        return Response.<Object>builder()
                .code(code.getCode())
                .info(code.getInfo())
                .build();
    }

}
