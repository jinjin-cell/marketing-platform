package cn.qijiv.trigger.http;

import cn.qijiv.trigger.api.IDCCService;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/dcc/")
public class DCCController implements IDCCService {

    @Resource
    private CuratorFramework client;

    private static final String BASE_CONFIG_PATH = "/market-platform-dcc";
    private static final String BASE_CONFIG_PATH_CONFIG = BASE_CONFIG_PATH + "/config";
    private static final String DEGRADE_SWITCH_KEY = "degradeSwitch";
    private static final Set<String> DEGRADE_SWITCH_VALUES = Collections.unmodifiableSet(
            new java.util.HashSet<>(Arrays.asList("open", "close")));

    /**
     * 更新配置
     * <p>
     * curl --request GET --url '<a href="http://localhost:8091/api/v1/raffle/dcc/update_config?key=degradeSwitch&value=close">...</a>'
     */
    @RequestMapping(value = "update_config", method = RequestMethod.GET)
    @Override
    public Response<Boolean> updateConfig(@RequestParam String key, @RequestParam String value) {
        try {
            if (!DEGRADE_SWITCH_KEY.equals(key) || !DEGRADE_SWITCH_VALUES.contains(value)) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("仅允许修改 degradeSwitch，值必须为 open 或 close")
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

}
