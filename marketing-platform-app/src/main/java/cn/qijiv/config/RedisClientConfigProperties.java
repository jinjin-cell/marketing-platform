package cn.qijiv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis client connection settings.
 */
@Data
@ConfigurationProperties(prefix = "redis")
public class RedisClientConfigProperties {

    /** Redis 服务器地址，如 redis://127.0.0.1:6379 */
    private String address;
    /** Redis 登录用户名（可选） */
    private String username;
    /** Redis 登录密码（可选） */
    private String password;
    /** 默认连接的数据库编号 */
    private Integer database = 0;
    /** 连接池大小 */
    private Integer connectionPoolSize = 32;
    /** 连接池最小空闲连接数 */
    private Integer connectionMinimumIdleSize = 8;
    /** 连接超时时间（毫秒） */
    private Integer connectTimeout = 10000;
    /** 读写超时时间（毫秒） */
    private Integer timeout = 3000;

}
