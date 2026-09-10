package cn.qijiv.infrastructure.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.api.hint.HintManager;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 数据库路由策略实现
 *
 * @author qijiv
 * @since 2026/7/30
 */
@Slf4j
@Component
public class DBRouterStrategy implements IDBRouterStrategy {

    @Resource
    private DBRouterConfig dbRouterConfig;

    /**
     * 根据用户ID计算库索引并设置分片路由，使后续数据库操作路由到指定数据库
     *
     * @param dbKey 路由键（如用户ID）
     */
    @Override
    public void doRouter(String dbKey) {
        // 扰动哈希取模分库，与 UserIdShardingAlgorithm 保持一致，保证同一用户路由到同一个库
        int dbIdx = Math.floorMod(dbKey.hashCode(), dbRouterConfig.getDbCount()) + 1;
        HintManager hintManager = HintManager.getInstance();
        hintManager.setDatabaseShardingValue(String.valueOf(dbIdx));
        log.debug("数据库路由 dbIdx {}", dbIdx);
    }

    /**
     * 手动设置分库路由
     *
     * @param dbIdx 库索引，从1开始
     */
    @Override
    public void setDBKey(int dbIdx) {
        HintManager hintManager = HintManager.getInstance();
        hintManager.setDatabaseShardingValue(String.valueOf(dbIdx));
        log.debug("手动设置分库路由 dbIdx {}", dbIdx);
    }

    /**
     * 手动设置分表路由
     * <p>
     * 当前 task 表每个库仅一张（不分表），分表路由无实际作用，仅保留接口兼容。
     * 后续如需分表，可在此处通过 HintManager.addTableShardingValue 设置表分片值。
     *
     * @param tbIdx 表索引，从0开始
     */
    @Override
    public void setTBKey(int tbIdx) {
        log.debug("手动设置分表路由 tbIdx {}", tbIdx);
    }

    /**
     * 获取分库数量
     *
     * @return 分库数量
     */
    @Override
    public int dbCount() {
        return dbRouterConfig.getDbCount();
    }

    /**
     * 清除当前线程的路由设置
     */
    @Override
    public void clear() {
        HintManager.clear();
    }

}
