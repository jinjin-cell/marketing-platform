package cn.qijiv.types.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用业务异常，携带业务错误码与错误信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 5317680961212299217L;

    /** 异常码 */
    private String code;

    /** 异常信息 */
    private String info;

    /**
     * 根据错误码构造异常
     *
     * @param code 错误码
     */
    public AppException(String code) {
        this.code = code;
    }

    /**
     * 根据错误码与原因构造异常
     *
     * @param code  错误码
     * @param cause 引发异常的原因
     */
    public AppException(String code, Throwable cause) {
        this.code = code;
        super.initCause(cause);
    }

    /**
     * 根据错误码与错误信息构造异常
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public AppException(String code, String message) {
        this.code = code;
        this.info = message;
    }

    /**
     * 根据错误码、错误信息与原因构造异常
     *
     * @param code    错误码
     * @param message 错误信息
     * @param cause   引发异常的原因
     */
    public AppException(String code, String message, Throwable cause) {
        this.code = code;
        this.info = message;
        super.initCause(cause);
    }

    /**
     * 返回异常的描述字符串，包含错误码与错误信息
     *
     * @return 异常描述字符串
     */
    @Override
    public String toString() {
        return "cn.qijiv.x.api.types.exception.XApiException{" +
                "code='" + code + '\'' +
                ", info='" + info + '\'' +
                '}';
    }

}
