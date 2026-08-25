package cn.qijiv.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the shared Redisson client used by infrastructure services.
 */
@Configuration
@EnableConfigurationProperties(RedisClientConfigProperties.class)
public class RedisClientConfig {

    /**
     * 构建 Redisson 客户端，根据配置属性设置服务器地址、认证信息与连接池等参数
     *
     * @param properties Redis 客户端配置属性
     * @return Redisson 客户端实例
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(RedisClientConfigProperties properties) {
        if (properties.getAddress() == null || properties.getAddress().trim().isEmpty()) {
            throw new IllegalStateException("redis.address must be configured");
        }

        Config config = new Config();
        // 使用 JSON 编解码器，避免缓存对象乱码或依赖 JDK 序列化
        config.setCodec(JsonJacksonCodec.INSTANCE);
        SingleServerConfig serverConfig = config
                .useSingleServer()
                .setAddress(properties.getAddress())
                .setDatabase(properties.getDatabase())
                .setConnectionPoolSize(properties.getConnectionPoolSize())
                .setConnectionMinimumIdleSize(properties.getConnectionMinimumIdleSize())
                .setConnectTimeout(properties.getConnectTimeout())
                .setTimeout(properties.getTimeout());

        if (properties.getUsername() != null && !properties.getUsername().trim().isEmpty()) {
            serverConfig.setUsername(properties.getUsername());
        }
        if (properties.getPassword() != null && !properties.getPassword().trim().isEmpty()) {
            serverConfig.setPassword(properties.getPassword());
        }

        return Redisson.create(config);
    }

}
