package com.wzb.rest.exception;

/**
 * 文件异常
 */
public class FileException extends Exception {

    /**
     * 文件异常
     *
     * @param message 消息
     */
    public FileException(String message) {
        super(message);
    }

    /**
     * 文件异常
     *
     * @param message 消息
     * @param cause   原因
     */
    public FileException(String message, Throwable cause) {
        super(message, cause);
    }
}
