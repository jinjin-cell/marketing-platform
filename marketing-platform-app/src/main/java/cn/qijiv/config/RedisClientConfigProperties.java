package cn.qijiv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis client connection settings.
 */
@Data
@ConfigurationProperties(prefix = "redis")
public class RedisClientConfigProperties {

    private String address;
    private String username;
    private String password;
    private Integer database = 0;
    private Integer connectionPoolSize = 32;
    private Integer connectionMinimumIdleSize = 8;
    private Integer connectTimeout = 10000;
    private Integer timeout = 3000;

}
