package cn.qijiv.config;

import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Sentinel 规则数据源：从 Nacos 拉取流控/熔断规则并注册到规则管理器，规则变更秒级热更新。
 *
 * <p>规则示例（Nacos 配置，group 默认 SENTINEL_GROUP）：
 * <pre>
 * dataId: marketing-platform-flow-rules
 * content: [{"resource":"activityDraw","grade":1,"count":2,"limitApp":"default","controlBehavior":0,"strategy":0}]
 *
 * dataId: marketing-platform-degrade-rules
 * content: [{"resource":"activityDraw","grade":1,"count":0.5,"timeWindow":10,"minRequestAmount":5,"statIntervalMs":1000}]
 * </pre>
 *
 * <p>Nacos 中无规则或数据为空时（fail-safe），Sentinel 默认放行所有请求。
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @Value("${NACOS_HOST:111.228.17.153}:${NACOS_PORT:8848}")
    private String nacosServerAddr;

    @Value("${sentinel.nacos.group:SENTINEL_GROUP}")
    private String nacosGroup;

    @Value("${sentinel.nacos.flow-data-id:marketing-platform-flow-rules}")
    private String flowDataId;

    @Value("${sentinel.nacos.degrade-data-id:marketing-platform-degrade-rules}")
    private String degradeDataId;

    @PostConstruct
    public void initSentinelRules() {
        // 流控规则数据源
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource =
                new NacosDataSource<>(nacosServerAddr, nacosGroup, flowDataId,
                        source -> JSON.parseArray(source, FlowRule.class));
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());

        // 熔断降级规则数据源
        ReadableDataSource<String, List<DegradeRule>> degradeRuleDataSource =
                new NacosDataSource<>(nacosServerAddr, nacosGroup, degradeDataId,
                        source -> JSON.parseArray(source, DegradeRule.class));
        DegradeRuleManager.register2Property(degradeRuleDataSource.getProperty());

        log.info("Sentinel 规则数据源已注册 nacos={} group={} flowDataId={} degradeDataId={}",
                nacosServerAddr, nacosGroup, flowDataId, degradeDataId);
    }

}
