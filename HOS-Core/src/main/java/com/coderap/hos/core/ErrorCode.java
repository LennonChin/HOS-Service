package com.coderap.hos.core;

/**
 * @program: HOS-Service
 * @description: 错误码
 * @author: Lennon Chin
 * @create: 2018/05/20 22:58:57
 */
public interface ErrorCode {

    public static final int ERROR_PERMISSION_DENIED = 403;
    public static final int ERROR_FILE_NOT_FOUND = 404;
    public static final int ERROR_HBASE = 500;
    public static final int ERROR_HDFS = 501;
}
