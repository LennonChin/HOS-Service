package com.coderap.hos.common;

import lombok.Data;
import okhttp3.Response;

import java.io.InputStream;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/22 20:20:32
 */
@Data
public class HOSObject {

    private ObjectMetaData metaData;
    private InputStream content;
    private Response response;

    public HOSObject() {
    }

    public HOSObject(Response response) {
        this.response = response;
    }

    // 释放资源
    public void close() {
        try {
            if (this.content != null) {
                this.content.close();
            }
            if (this.response != null) {
                this.response.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
