package cn.qijiv.types.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 统一响应体，封装接口返回的数据、状态码与提示信息
 *
 * @param <T> 响应数据类型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Response<T> implements Serializable {

    /** 响应数据 */
    private T data;
    /** 响应状态码 */
    private String code;
    /** 响应提示信息 */
    private String info;
}
