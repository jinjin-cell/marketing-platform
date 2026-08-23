package cn.qijiv.infrastructure.persistent.db;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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

    /** 分库数量，对应数据源 ds1、ds2（ds0 为配置库，不参与分片） */
    private int dbCount = 2;

    /** 每库分表数量，对应 actual-data-nodes 的 _00$->{0..3}（order / award_record / raffle_order 三张分表） */
    private int tableCount = 4;

    /**
     * 静态快照：ShardingSphere 通过反射（非 Spring 容器）实例化分片算法，
     * 算法类无法注入本 Bean，因此容器初始化时把值发布为静态字段供其读取。
     */
    private static volatile int staticDbCount = 2;
    private static volatile int staticTableCount = 4;

    @PostConstruct
    public void publishSnapshot() {
        staticDbCount = dbCount;
        staticTableCount = tableCount;
    }

    /** 供分片算法读取的分库数量 */
    public static int dbCount() {
        return staticDbCount;
    }

    /** 供分片算法读取的每库分表数量 */
    public static int tableCount() {
        return staticTableCount;
    }

}
