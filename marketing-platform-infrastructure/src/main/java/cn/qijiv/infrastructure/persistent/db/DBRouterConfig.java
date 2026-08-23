package cn.qijiv.infrastructure.persistent.db;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 分库分表路由配置
 *
 * @author qijiv
 * @since 2026/8/19
 */
@Data
@Component
@ConfigurationProperties(prefix = "db-router")
public class DBRouterConfig {

    /** 分库数量 */
    private int dbCount = 2;

    /** 分表数量 */
    private int tbCount = 1;

}
