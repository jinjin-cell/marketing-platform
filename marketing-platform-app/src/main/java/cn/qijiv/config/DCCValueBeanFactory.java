package cn.qijiv.config;

import cn.qijiv.types.annotations.DCCValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 基于 Zookeeper 的配置中心实现原理：
 * <ul>
 *   <li>为每个标注 {@link DCCValue} 的字段在 {@code /market-platform-dcc/config/<key>} 建立节点；</li>
 *   <li>节点不存在时创建节点并<b>把默认值写入节点</b>（而非只写内存），保证应用重启后仍能读到默认值，
 *       避免空节点导致字段为 null（例如 degradeSwitch 为 null 时抽奖会被误判为降级）；</li>
 *   <li>监听节点变更（NODE_CHANGED / NODE_CREATED），动态热更新到目标 Bean 字段。</li>
 * </ul>
 * <p><b>启动异步化</b>：ZK 会话建立依赖远程网络，可能耗时数十秒。Bean 初始化阶段只写入默认值并登记同步任务，
 * ZK 连接、节点创建/读取、缓存监听全部由后台守护线程 {@code dcc-zk-init} 完成；ZK 长期不可用时应用以默认值运行，不阻塞启动。</p>
 */
@Slf4j
@Configuration
public class DCCValueBeanFactory implements BeanPostProcessor {

    private static final String BASE_CONFIG_PATH = "/market-platform-dcc";
    private static final String BASE_CONFIG_PATH_CONFIG = BASE_CONFIG_PATH + "/config";

    private final CuratorFramework client;

    /** 节点路径 -> 字段所在的目标对象（AOP 代理背后的单例目标或 Bean 本体） */
    private final Map<String, Object> dccObjGroup = new ConcurrentHashMap<>();

    /** 待同步的 DCC 字段任务：ZK 连接建立后由后台线程消费 */
    private final BlockingQueue<DccSyncTask> syncQueue = new LinkedBlockingQueue<>();

    public DCCValueBeanFactory(CuratorFramework client) {
        this.client = client;
        // ZK 会话建立是远程网络操作（可能耗时数十秒），整个初始化放后台线程，不阻塞 Spring 启动
        Thread dccInitThread = new Thread(this::initAsync, "dcc-zk-init");
        dccInitThread.setDaemon(true);
        dccInitThread.start();
    }

    /**
     * 后台初始化：等待 ZK 连接 → 确保基础节点 → 启动缓存监听 → 持续消费字段同步队列。
     * 队列需持续消费：Bean 的注册可能晚于连接建立，不能连接后一次性排空就退出。
     */
    private void initAsync() {
        try {
            client.blockUntilConnected();
            log.info("DCC Zookeeper 连接建立，开始异步初始化配置监听");

            // 节点判断
            if (null == client.checkExists().forPath(BASE_CONFIG_PATH_CONFIG)) {
                client.create().creatingParentsIfNeeded().forPath(BASE_CONFIG_PATH_CONFIG);
                log.info("DCC 节点监听 base node {} not absent create new done!", BASE_CONFIG_PATH_CONFIG);
            }

            CuratorCache curatorCache = CuratorCache.build(client, BASE_CONFIG_PATH_CONFIG);
            curatorCache.start();

            curatorCache.listenable().addListener((type, oldData, data) -> {
                if (type != CuratorCacheListener.Type.NODE_CHANGED && type != CuratorCacheListener.Type.NODE_CREATED) {
                    return;
                }
                if (data == null || data.getData() == null) {
                    return;
                }
                String dccValuePath = data.getPath();
                Object objBean = dccObjGroup.get(dccValuePath);
                if (null == objBean) {
                    return;
                }
                String configValue = new String(data.getData(), StandardCharsets.UTF_8);
                try {
                    Field field = findField(objBean.getClass(), dccValuePath.substring(dccValuePath.lastIndexOf("/") + 1));
                    if (field == null) {
                        log.warn("DCC 节点监听 未找到字段 path:{}", dccValuePath);
                        return;
                    }
                    field.setAccessible(true);
                    field.set(objBean, configValue);
                    field.setAccessible(false);
                    log.info("DCC 节点监听 热更新配置 {} = {}", dccValuePath, configValue);
                } catch (Exception e) {
                    log.error("DCC 节点监听 热更新失败 path:{}", dccValuePath, e);
                }
            });

            // 持续消费 DCC 字段同步任务
            while (!Thread.currentThread().isInterrupted()) {
                syncFieldQuietly(syncQueue.take());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("DCC 异步初始化线程被中断");
        } catch (Exception e) {
            // ZK 长期不可用：应用以默认值继续运行，仅失去动态配置能力
            log.error("DCC 异步初始化失败，动态配置中心不可用", e);
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 注意；增加 AOP 代理后，获得类的方式要通过 AopUtils.getTargetClass(bean); 不能直接 bean.class 因为代理后类的结构发生变化，这样不能获得到自己的自定义注解了。
        Class<?> targetBeanClass = bean.getClass();
        Object targetBeanObject = bean;
        if (AopUtils.isAopProxy(bean)) {
            targetBeanClass = AopUtils.getTargetClass(bean);
            Object singletonTarget = AopProxyUtils.getSingletonTarget(bean);
            if (null != singletonTarget) {
                targetBeanObject = singletonTarget;
            }
        }

        Field[] fields = targetBeanClass.getDeclaredFields();
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
            String key = splits[0].trim();
            String defaultValue = splits.length == 2 ? splits[1] : null;

            String keyPath = BASE_CONFIG_PATH_CONFIG.concat("/").concat(key);

            // 先写入默认值，保证 ZK 未连接期间 Bean 也有可用配置（尤其是降级/限流开关）
            if (StringUtils.isNotBlank(defaultValue)) {
                try {
                    setField(targetBeanObject, field, defaultValue);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            // 注册热更新目标（同步执行，保证监听事件到达时一定能找到 Bean）
            dccObjGroup.put(keyPath, targetBeanObject);

            // ZK 节点创建/读取/回写为远程 I/O，交由后台线程在连接建立后执行
            syncQueue.offer(new DccSyncTask(keyPath, targetBeanObject, field, defaultValue));
        }
        return bean;
    }

    /**
     * 单个 DCC 字段与 ZK 节点同步（后台线程执行），失败仅记录日志、字段保留默认值，不影响其他字段
     */
    private void syncFieldQuietly(DccSyncTask task) {
        try {
            // 判断当前节点是否存在，不存在则创建出 Zookeeper 节点并把默认值写入节点
            if (null == client.checkExists().forPath(task.keyPath)) {
                if (StringUtils.isNotBlank(task.defaultValue)) {
                    client.create().creatingParentsIfNeeded().forPath(task.keyPath, task.defaultValue.getBytes(StandardCharsets.UTF_8));
                    log.info("DCC 节点监听 创建节点(带默认值) {} = {}", task.keyPath, task.defaultValue);
                } else {
                    client.create().creatingParentsIfNeeded().forPath(task.keyPath);
                    log.info("DCC 节点监听 创建节点 {}", task.keyPath);
                }
                return;
            }
            String configValue = new String(client.getData().forPath(task.keyPath), StandardCharsets.UTF_8);
            if (StringUtils.isBlank(configValue) && StringUtils.isNotBlank(task.defaultValue)) {
                // 节点存在但数据为空：回写默认值并应用到字段，保证重启后读取一致、字段不为 null
                client.setData().forPath(task.keyPath, task.defaultValue.getBytes(StandardCharsets.UTF_8));
                setField(task.targetBean, task.field, task.defaultValue);
                log.info("DCC 节点监听 节点数据为空回写默认值 {} = {}", task.keyPath, task.defaultValue);
            } else {
                setField(task.targetBean, task.field, configValue);
                log.info("DCC 节点监听 设置配置 {} {} {}", task.keyPath, task.field.getName(), configValue);
            }
        } catch (Exception e) {
            log.error("DCC 字段同步失败 path:{}，字段保留默认值", task.keyPath, e);
        }
    }

    /** DCC 字段同步任务 */
    private static class DccSyncTask {
        private final String keyPath;
        private final Object targetBean;
        private final Field field;
        private final String defaultValue;

        private DccSyncTask(String keyPath, Object targetBean, Field field, String defaultValue) {
            this.keyPath = keyPath;
            this.targetBean = targetBean;
            this.field = field;
            this.defaultValue = defaultValue;
        }
    }

    private void setField(Object target, Field field, String value) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(target, value);
        field.setAccessible(false);
    }

    /**
     * 沿继承链向上查找字段，兼容 CGLIB 代理子类与父类字段
     */
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
