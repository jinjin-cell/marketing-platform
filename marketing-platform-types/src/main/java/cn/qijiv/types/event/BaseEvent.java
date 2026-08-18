package cn.qijiv.types.event;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Date;

/**
 * 事件基类，定义事件消息构建与消息主题的抽象规范
 *
 * @param <T> 事件业务数据类型
 */
@Data
public abstract class BaseEvent<T> {

    /**
     * 根据业务数据构建事件消息
     *
     * @param data 事件业务数据
     * @return 事件消息
     */
    public abstract EventMessage<T> buildEventMessage(T data);

    /**
     * 获取事件发布主题
     *
     * @return 消息主题
     */
    public abstract String topic();

    /**
     * 事件消息体，封装消息唯一标识、时间戳与业务数据
     *
     * @param <T> 业务数据类型
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EventMessage<T> {
        /** 消息唯一标识 */
        private String id;
        /** 消息时间戳 */
        private Date timestamp;
        /** 业务数据 */
        private T data;
    }

}

