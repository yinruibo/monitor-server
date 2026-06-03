package cn.hongt.monitor.server.common.exception;

/**
 * @author yrb
 * @date 2021/7/1
 * @Description: ParamUtils用
 */
public class InvalidArgumentException extends RuntimeException{

    public InvalidArgumentException() {
    }

    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidArgumentException(Throwable cause) {
        super(cause);
    }
}
