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

    @Override
    public void doRouter(String dbKey) {
        HintManager hintManager = HintManager.getInstance();
        hintManager.setDatabaseShardingValue(dbKey);
    }

    @Override
    public void clear() {
        HintManager.clear();
    }

}
