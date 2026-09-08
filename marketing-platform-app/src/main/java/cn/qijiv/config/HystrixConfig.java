package cn.qijiv.config;

import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hystrix 熔断支持：
 * 注册 {@link HystrixCommandAspect}，使标注 {@code @HystrixCommand} 的方法
 * 具备线程隔离、超时保护与熔断能力，异常/超时时自动执行 fallbackMethod。
 */
@Configuration
public class HystrixConfig {

    @Bean
    public HystrixCommandAspect hystrixCommandAspect() {
        return new HystrixCommandAspect();
    }

}
