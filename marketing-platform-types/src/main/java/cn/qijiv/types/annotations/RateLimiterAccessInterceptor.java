package cn.qijiv.types.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface RateLimiterAccessInterceptor {

    /** 用哪个字段作为拦截标识，未配置则默认走全部（共享限流桶，且不进入黑名单） */
    String key() default "all";

    /** 限制频次（每秒请求次数） */
    double permitsPerSecond();

    /**
     * 黑名单阈值：用户被限流拦截的累计次数达到该值后进入黑名单（黑名单内请求 24h 内直接拦截）。
     * 0 表示不启用黑名单
     */
    double blacklistCount() default 0;

    /** 拦截后的执行方法 */
    String fallbackMethod();

}

