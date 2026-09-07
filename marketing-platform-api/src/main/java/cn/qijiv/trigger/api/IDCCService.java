package cn.qijiv.trigger.api;

import cn.qijiv.types.model.Response;

/**
 * @author qijiv
 * @since  2026/09/07
 */
public interface IDCCService {

    Response<Boolean> updateConfig(String key, String value);

}
