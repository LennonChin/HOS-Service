package com.coderap.hos.core;

/**
 * @program: HOS-Service
 * @description: 异常基类
 * @author: Lennon Chin
 * @create: 2018/05/20 22:56:44
 */
public abstract class HOSException extends RuntimeException {

    protected String errorMessage;

    public HOSException(String errorMessage, Throwable cause) {

        super(cause);
        this.errorMessage = errorMessage;
    }

    public abstract int errorCode();

    public String errorMessage() {
        return errorMessage;
    }
}
