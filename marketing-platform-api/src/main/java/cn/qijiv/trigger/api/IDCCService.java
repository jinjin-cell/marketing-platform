package cn.qijiv.trigger.api;

import cn.qijiv.types.model.Response;

import java.util.Map;

/**
 * @author qijiv
 * @since  2026/09/07
 */
public interface IDCCService {

    Response<Boolean> updateConfig(String key, String value, String token);

    /**
     * 查询当前 DCC 开关配置
     *
     * @return 开关名 → 当前值（open / close）
     */
    Response<Map<String, String>> queryConfig(String token);

}
