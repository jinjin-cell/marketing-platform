package cn.qijiv.aop;

import cn.qijiv.types.annotations.DCCValue;
import cn.qijiv.types.annotations.RateLimiterAccessInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RScript;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 分布式动态限流 AOP。
 *
 * <p>与旧版「单机 Guava」实现不同：
 * <ol>
 *   <li>限流器改为 Redis 分布式令牌桶（Redisson RRateLimiter），同一用户在多实例间共享同一个限流状态；</li>
 *   <li>黑名单计数落在 Redis，24h 过期，多实例共享；</li>
 *   <li>限流开关仍由 DCC（Zookeeper 配置中心）动态下发 rateLimiterSwitch。</li>
 * </ol>
 *
 * <p>开关语义：rateLimiterSwitch 为 open 时才走限流策略；close 或未配置时直接放行。
 */
@Slf4j
@Aspect
@Component
@Order(0)
public class RateLimiterAOP {

    @DCCValue("rateLimiterSwitch:close")
    private String rateLimiterSwitch;

    /** 限流器 Redis key 前缀（多实例共享，需跨应用唯一） */
    private static final String RATE_LIMITER_KEY_PREFIX = "marketing:ratelimiter:";
    /** 黑名单 Redis key 前缀 */
    private static final String BLACKLIST_KEY_PREFIX = "marketing:ratelimiter:blacklist:";
    /** 黑名单有效期：24 小时 */
    private static final long BLACKLIST_TTL_SECONDS = 24 * 60 * 60L;
    /** 限流器空闲失效时间：与旧版 Guava 缓存 1 分钟一致，避免 Redis key 无限增长 */
    private static final long RATE_LIMITER_IDLE_SECONDS = 60L;

    /** 拦截计数 +1，首次写入时设置 24h 过期（原子） */
    private static final String INCR_BLACKLIST_LUA =
            "local c = redis.call('incr', KEYS[1]) " +
                    "if c == 1 then redis.call('expire', KEYS[1], ARGV[1]) end " +
                    "return c";
    /** 读取黑名单计数，不存在返回 0 */
    private static final String GET_BLACKLIST_LUA =
            "local c = redis.call('get', KEYS[1]) " +
                    "if not c then return 0 end " +
                    "return c";

    private final RedissonClient redissonClient;

    public RateLimiterAOP(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Pointcut("@annotation(cn.qijiv.types.annotations.RateLimiterAccessInterceptor)")
    public void aopPoint() {
    }

    @Around("aopPoint() && @annotation(rateLimiterAccessInterceptor)")
    public Object doRouter(ProceedingJoinPoint jp, RateLimiterAccessInterceptor rateLimiterAccessInterceptor) throws Throwable {
        // 0. 限流开关【open 开启、close 关闭】关闭后，不会走限流策略
        if (StringUtils.isBlank(rateLimiterSwitch) || "close".equals(rateLimiterSwitch)) {
            return jp.proceed();
        }

        String key = rateLimiterAccessInterceptor.key();
        if (StringUtils.isBlank(key)) {
            throw new RuntimeException("annotation RateLimiter key is null！");
        }

        // 获取拦截字段值；解析不到时统一收敛到 "all"（共享限流桶，且不参与黑名单）
        String keyAttr = getAttrValue(key, jp.getArgs());
        log.info("aop attr {}", keyAttr);

        double blacklistCount = rateLimiterAccessInterceptor.blacklistCount();
        boolean allowBlacklist = blacklistCount > 0 && !"all".equals(keyAttr);
        boolean blocked = false;
        boolean limited = false;

        try {
            // 1. 黑名单拦截（Redis 分布式，24h）
            blocked = allowBlacklist && isBlacklisted(keyAttr, blacklistCount);
            // 2. 分布式令牌桶限流
            if (!blocked && !tryAcquire(keyAttr, rateLimiterAccessInterceptor.permitsPerSecond())) {
                limited = true;
                // 3. 被限流一次，黑名单计数 +1（达到阈值后进入 24h 黑名单）
                if (allowBlacklist) {
                    try {
                        incrementBlacklist(keyAttr);
                    } catch (Exception ex) {
                        log.error("限流-黑名单计数累加失败 attr：{}", keyAttr, ex);
                    }
                }
            }
        } catch (Exception e) {
            // 限流组件（Redis）异常时 fail-open 放行，避免限流器拖垮业务
            log.error("限流执行异常，本次请求放行 attr：{}", keyAttr, e);
            return jp.proceed();
        }

        // 4. 命中拦截策略，执行用户配置的 fallback
        if (blocked) {
            log.info("限流-黑名单拦截(24h)：{}", keyAttr);
            return fallbackMethodResult(jp, rateLimiterAccessInterceptor.fallbackMethod());
        }
        if (limited) {
            log.info("限流-超频次拦截：{}", keyAttr);
            return fallbackMethodResult(jp, rateLimiterAccessInterceptor.fallbackMethod());
        }

        // 5. 放行
        return jp.proceed();
    }

    /**
     * 是否已进入黑名单：累计被限流拦截次数达到 blacklistCount 即进入黑名单（24h）。
     */
    private boolean isBlacklisted(String keyAttr, double blacklistCount) {
        long count = evalInt(GET_BLACKLIST_LUA, BLACKLIST_KEY_PREFIX + keyAttr);
        return count >= (long) Math.ceil(blacklistCount);
    }

    /**
     * 黑名单计数 +1，首次写入设置 24h 过期。
     */
    private void incrementBlacklist(String keyAttr) {
        evalInt(INCR_BLACKLIST_LUA, BLACKLIST_KEY_PREFIX + keyAttr, BLACKLIST_TTL_SECONDS);
    }

    /**
     * 分布式令牌桶限流：每秒补充 permitsPerSecond 个令牌。
     *
     * <p>key 空闲 60 秒后过期，等价旧版 Guava 缓存 1 分钟失效的语义。
     */
    private boolean tryAcquire(String keyAttr, double permitsPerSecond) {
        String key = RATE_LIMITER_KEY_PREFIX + keyAttr;
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        long permits = Math.max(1L, (long) Math.ceil(permitsPerSecond));
        // 幂等设置限流速率：配置已存在则 no-op，key 过期后会重新初始化
        rateLimiter.trySetRate(RateType.OVERALL, permits, 1, RateIntervalUnit.SECONDS);
        boolean acquired = rateLimiter.tryAcquire();
        if (acquired) {
            // 活跃期间刷新 TTL；空闲 60s 后 key 自动过期（限流状态重置）
            rateLimiter.expire(RATE_LIMITER_IDLE_SECONDS, TimeUnit.SECONDS);
        }
        return acquired;
    }

    private long evalInt(String lua, String key, Object... argv) {
        Number n = redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE, lua, RScript.ReturnType.INTEGER,
                        Collections.singletonList(key), argv);
        return n == null ? 0L : n.longValue();
    }

    /**
     * 调用用户配置的回调方法，当拦截后，返回回调结果。
     */
    private Object fallbackMethodResult(JoinPoint jp, String fallbackMethod) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Signature sig = jp.getSignature();
        MethodSignature methodSignature = (MethodSignature) sig;
        Method method = jp.getTarget().getClass().getMethod(fallbackMethod, methodSignature.getParameterTypes());
        return method.invoke(jp.getThis(), jp.getArgs());
    }

    /**
     * 根据拦截字段名从方法入参中解析出属性值。
     *
     * <p>入参为空、入参为 null、字段不存在或字段值为空时，统一收敛为 "all"：
     * 使用全局共享限流桶兜底（key 为 all 时不会进入黑名单），避免空值引发 NPE。
     */
    public String getAttrValue(String attr, Object[] args) {
        if (args == null || args.length == 0) {
            return "all";
        }
        if (args[0] instanceof String) {
            return StringUtils.defaultIfBlank((String) args[0], "all");
        }
        Object value = null;
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            try {
                value = getValueByName(arg, attr);
            } catch (Exception e) {
                log.error("获取路由属性值失败 attr：{}", attr, e);
                continue;
            }
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                break;
            }
            value = null;
        }
        return (value == null || StringUtils.isBlank(String.valueOf(value))) ? "all" : String.valueOf(value);
    }

    /**
     * 获取对象的特定属性值
     *
     * @param item 对象
     * @param name 属性名
     * @return 属性值
     */
    private Object getValueByName(Object item, String name) {
        Field field = getFieldByName(item, name);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            Object o = field.get(item);
            field.setAccessible(false);
            return o;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 根据名称获取字段，该方法同时兼顾继承类获取父类的属性
     */
    private Field getFieldByName(Object item, String name) {
        try {
            Field field;
            try {
                field = item.getClass().getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                field = item.getClass().getSuperclass().getDeclaredField(name);
            }
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

}
