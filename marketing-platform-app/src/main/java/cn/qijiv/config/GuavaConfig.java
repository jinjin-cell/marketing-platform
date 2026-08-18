package cn.qijiv.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Guava 本地缓存配置类，提供短时效的本地缓存 Bean
 */
@Configuration
public class GuavaConfig {

    /**
     * 构建 Guava 缓存，写入后 3 秒过期
     *
     * @return Guava 缓存实例
     */
    @Bean(name = "cache")
    public Cache<String, String> cache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .build();
    }

}
