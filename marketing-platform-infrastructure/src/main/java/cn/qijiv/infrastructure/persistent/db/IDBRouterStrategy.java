package cn.qijiv.infrastructure.persistent.db;

/**
 * 数据库路由策略接口
 *
 * @author qijiv
 * @since 2026/7/30
 */
public interface IDBRouterStrategy {

    /**
     * 手动设置分片路由，确保后续操作在同一个数据库连接中执行
     *
     * @param dbKey 路由键（如用户ID）
     */
    void doRouter(String dbKey);

    /**
     * 清除路由设置
     */
    void clear();

}
