package cn.qijiv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用通用配置属性，对应配置前缀 app.config
 */
@Data
@ConfigurationProperties(prefix = "app.config")
public class AppConfigProperties {

    /** 版本，方便通过接口版本升级 */
    private String apiVersion = "v1";

    /** 跨域，开发阶段可以设置为 * 不限制 */
    private String crossOrigin = "*";

}
