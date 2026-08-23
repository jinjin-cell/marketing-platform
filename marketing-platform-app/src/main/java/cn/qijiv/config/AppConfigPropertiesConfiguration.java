package cn.qijiv.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用通用配置属性启用类
 */
@Configuration
@EnableConfigurationProperties(AppConfigProperties.class)
public class AppConfigPropertiesConfiguration {
}
