package cn.qijiv.trigger.http;

import cn.qijiv.trigger.api.IDCCService;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/dcc/")
public class DCCController implements IDCCService {

    @Resource
    private CuratorFramework client;

    @Value("${app.config.admin-token}")
    private String adminToken;

    private static final String BASE_CONFIG_PATH = "/market-platform-dcc";
    private static final String BASE_CONFIG_PATH_CONFIG = BASE_CONFIG_PATH + "/config";

    /** 允许通过 DCC 接口动态修改的开关键 */
    private static final Set<String> DCC_SWITCH_KEYS = Collections.unmodifiableSet(
            new java.util.HashSet<>(Arrays.asList("degradeSwitch", "rateLimiterSwitch")));
    /** 开关允许的值 */
    private static final Set<String> DCC_SWITCH_VALUES = Collections.unmodifiableSet(
            new java.util.HashSet<>(Arrays.asList("open", "close")));

    /** 节点不存在时的默认值，与注解 @DCCValue 保持一致 */
    private static final Map<String, String> DCC_SWITCH_DEFAULTS;

    static {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("degradeSwitch", "open");
        defaults.put("rateLimiterSwitch", "close");
        DCC_SWITCH_DEFAULTS = Collections.unmodifiableMap(defaults);
    }

    /**
     * 查询当前 DCC 开关值，供运维页展示。
     *
     * <p>接口：{@code /api/v1/raffle/dcc/query_config}
     * <br>示例：{@code curl --request GET --url 'http://localhost:8091/api/v1/raffle/dcc/query_config'}
     */
    @RequestMapping(value = "query_config", method = RequestMethod.GET)
    @Override
    public Response<Map<String, String>> queryConfig(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        try {
            if (!isAuthorized(token)) {
                log.warn("DCC 配置查询被拒绝，运维令牌不正确");
                return unauthorized();
            }
            Map<String, String> configs = new LinkedHashMap<>();
            for (String key : DCC_SWITCH_KEYS) {
                String keyPath = BASE_CONFIG_PATH_CONFIG.concat("/").concat(key);
                String value = DCC_SWITCH_DEFAULTS.get(key);
                try {
                    if (null != client.checkExists().forPath(keyPath)) {
                        byte[] data = client.getData().forPath(keyPath);
                        if (null != data && data.length > 0) {
                            value = new String(data, StandardCharsets.UTF_8);
                        }
                    }
                } catch (Exception e) {
                    log.warn("读取 DCC 配置失败，返回默认值 key:{}", key, e);
                }
                configs.put(key, value);
            }
            log.info("查询 DCC 配置完成 configs:{}", configs);
            return Response.<Map<String, String>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(configs)
                    .build();
        } catch (Exception e) {
            log.error("查询 DCC 配置失败", e);
            return Response.<Map<String, String>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 更新配置
     * <p>
     * 写操作只接受 POST，运维令牌通过 {@code X-Admin-Token} 请求头传递。
     */
    @RequestMapping(value = "update_config", method = RequestMethod.POST)
    @Override
    public Response<Boolean> updateConfig(
            @RequestParam String key,
            @RequestParam String value,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        try {
            if (!isAuthorized(token)) {
                log.warn("DCC 配置变更被拒绝，运维令牌不正确 key:{}", key);
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("无权执行运维操作")
                        .data(false)
                        .build();
            }
            if (!DCC_SWITCH_KEYS.contains(key) || !DCC_SWITCH_VALUES.contains(value)) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("仅允许修改 " + DCC_SWITCH_KEYS + "，值必须为 open 或 close")
                        .data(false)
                        .build();
            }
            log.info("DCC 动态配置值变更开始 key:{} value:{}", key, value);
            String keyPath = BASE_CONFIG_PATH_CONFIG.concat("/").concat(key);
            if (null == client.checkExists().forPath(keyPath)) {
                client.create().creatingParentsIfNeeded().forPath(keyPath);
                log.info("DCC 节点监听 base node {} not absent create new done!", keyPath);
            }
            Stat stat = client.setData().forPath(keyPath, value.getBytes(StandardCharsets.UTF_8));
            log.info("DCC 动态配置值变更完成 key:{} value:{} time:{}", key, value, stat.getCtime());
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
        } catch (Exception e) {
            log.error("DCC 动态配置值变更失败 key:{} value:{}", key, value, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }

    private boolean isAuthorized(String token) {
        return token != null && adminToken != null && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), adminToken.getBytes(StandardCharsets.UTF_8));
    }

    private Response<Map<String, String>> unauthorized() {
        return Response.<Map<String, String>>builder()
                .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                .info("无权执行运维操作")
                .build();
    }

}
