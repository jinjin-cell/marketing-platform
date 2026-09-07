package cn.qijiv.config;

import cn.qijiv.types.annotations.DCCValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Zookeeper 的配置中心实现原理
 */
@Slf4j
@Configuration
public class DCCValueBeanFactory implements BeanPostProcessor {

    private static final String BASE_CONFIG_PATH = "/market-platform-dcc";
    private static final String BASE_CONFIG_PATH_CONFIG = BASE_CONFIG_PATH + "/config";

    private final CuratorFramework client;

    private final Map<String, Object> dccObjGroup = new ConcurrentHashMap<>();

    public DCCValueBeanFactory(CuratorFramework client) throws Exception {
        this.client = client;

        // 节点判断
        if (null == client.checkExists().forPath(BASE_CONFIG_PATH_CONFIG)) {
            client.create().creatingParentsIfNeeded().forPath(BASE_CONFIG_PATH_CONFIG);
            log.info("DCC 节点监听 base node {} not absent create new done!", BASE_CONFIG_PATH_CONFIG);
        }

        CuratorCache curatorCache = CuratorCache.build(client, BASE_CONFIG_PATH_CONFIG);
        curatorCache.start();

        curatorCache.listenable().addListener((type, oldData, data) -> {
            switch (type) {
                case NODE_CREATED:
                case NODE_CHANGED:
                    if (data == null || data.getData() == null) return;
                    String dccValuePath = data.getPath();
                    Object objBean = dccObjGroup.get(dccValuePath);
                    if (null == objBean) return;
                    try {
                        Field field = findField(objBean.getClass(), dccValuePath.substring(dccValuePath.lastIndexOf("/") + 1));
                        if (field != null) {
                            field.setAccessible(true);
                            field.set(objBean, new String(data.getData(), StandardCharsets.UTF_8));
                            field.setAccessible(false);
                        }
                    } catch (Exception e) {
                        log.error("DCC 节点监听更新字段失败 path:{}", dccValuePath, e);
                    }
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(DCCValue.class)) {
                continue;
            }

            DCCValue dccValue = field.getAnnotation(DCCValue.class);

            String value = dccValue.value();
            if (StringUtils.isBlank(value)) {
                throw new RuntimeException(field.getName() + " @DCCValue is not config value config case 「isSwitch/isSwitch:1」");
            }

            String[] splits = value.split(":", 2);
            String key = splits[0];
            String defaultValue = splits.length == 2 ? splits[1] : null;
            if (StringUtils.isBlank(key)) {
                throw new IllegalArgumentException(field.getName() + " @DCCValue key must not be blank");
            }

            try {
                // 判断当前节点是否存在，不存在则创建出 Zookeeper 节点
                String keyPath = BASE_CONFIG_PATH_CONFIG.concat("/").concat(key);
                if (null == client.checkExists().forPath(keyPath)) {
                    // 创建节点时直接写入默认值（避免节点为空；否则应用重启后字段读不到值而归为 null，导致 draw() 被误判为降级）
                    if (StringUtils.isNotBlank(defaultValue)) {
                        client.create().creatingParentsIfNeeded().forPath(keyPath, defaultValue.getBytes(StandardCharsets.UTF_8));
                        field.setAccessible(true);
                        field.set(bean, defaultValue);
                        field.setAccessible(false);
                        log.info("DCC 节点监听 创建节点(带默认值) {} = {}", keyPath, defaultValue);
                    } else {
                        client.create().creatingParentsIfNeeded().forPath(keyPath);
                        log.info("DCC 节点监听 创建节点 {}", keyPath);
                    }
                } else {
                    String configValue = new String(client.getData().forPath(keyPath), StandardCharsets.UTF_8);
                    if (StringUtils.isNotBlank(configValue)) {
                        field.setAccessible(true);
                        field.set(bean, configValue);
                        field.setAccessible(false);
                        log.info("DCC 节点监听 设置配置 {} {} {}", keyPath, field.getName(), configValue);
                    } else if (StringUtils.isNotBlank(defaultValue)) {
                        // 节点存在但数据为空：回退默认值并写回，防止重启后字段为 null 导致活动被误降级
                        client.setData().forPath(keyPath, defaultValue.getBytes(StandardCharsets.UTF_8));
                        field.setAccessible(true);
                        field.set(bean, defaultValue);
                        field.setAccessible(false);
                        log.info("DCC 节点监听 节点数据为空回退默认值 {} {} {}", keyPath, field.getName(), defaultValue);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            dccObjGroup.put(BASE_CONFIG_PATH_CONFIG.concat("/").concat(key), bean);
        }
        return bean;
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

}
