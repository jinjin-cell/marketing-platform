package cn.qijiv.infrastructure.db;

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
     * 手动设置分库路由
     *
     * @param dbIdx 库索引，从1开始
     */
    void setDBKey(int dbIdx);

    /**
     * 手动设置分表路由
     *
     * @param tbIdx 表索引，从0开始
     */
    void setTBKey(int tbIdx);

    /**
     * 获取分库数量
     *
     * @return 分库数量
     */
    int dbCount();

    /**
     * 清除路由设置
     */
    void clear();

}
