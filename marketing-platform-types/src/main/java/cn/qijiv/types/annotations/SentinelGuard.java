package cn.qijiv.types.annotations;

import java.lang.annotation.*;

/**
 * Sentinel 熔断降级标记：标注的方法会进入 Sentinel 资源保护。
 *
 * <ul>
 *   <li>{@link #value()} 为 Sentinel 资源名（缺省取方法签名）；</li>
 *   <li>命中流控/熔断规则时，由 {@code SentinelGuardAOP} 统一返回 HYSTRIX(0006) 响应码；</li>
 *   <li>若 DCC 降级开关 degradeSwitch 非 open，则直接返回 DEGRADE_SWITCH(0004)，不进入 Sentinel。</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface SentinelGuard {

    /** Sentinel 资源名 */
    String value() default "";

}
