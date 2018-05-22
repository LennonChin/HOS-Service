package com.coderap.hos.common;

import lombok.Data;

import java.util.Map;

/**
 * @program: HOS-Service
 * @description: 对象meta信息类
 * @author: Lennon Chin
 * @create: 2018/05/22 20:14:29
 */
@Data
public class ObjectMetaData {

    private String bucket;
    private String key;
    private String mediaType;
    private long length;
    private long lastModifyTime;
    private Map<String, String> attrs;

    public String getContentEncoding() {
        return this.attrs != null ? this.attrs.get("content-encoding") : null;
    }
}
