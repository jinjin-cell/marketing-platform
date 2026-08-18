package cn.qijiv.infrastructure.persistent.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.api.hint.HintManager;
import org.springframework.stereotype.Component;

/**
 * 数据库路由策略实现
 *
 * @author qijiv
 * @since 2026/7/30
 */
@Slf4j
@Component
public class DBRouterStrategy implements IDBRouterStrategy {

    /**
     * 设置数据库分片路由，使后续数据库操作路由到指定数据库
     *
     * @param dbKey 路由键（如用户ID）
     */
    @Override
    public void doRouter(String dbKey) {
        HintManager hintManager = HintManager.getInstance();
        hintManager.setDatabaseShardingValue(dbKey);
    }

    /**
     * 清除当前线程的路由设置
     */
    @Override
    public void clear() {
        HintManager.clear();
    }

}
