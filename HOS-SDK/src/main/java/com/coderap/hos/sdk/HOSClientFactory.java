package com.coderap.hos.sdk;

import com.coderap.hos.sdk.service.HOSClientService;
import com.coderap.hos.sdk.service.impl.HOSClientServiceImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: HOS-Service
 * @description: 创建HOSClient的工厂类
 * @author: Lennon Chin
 * @create: 2018/05/24 21:00:56
 */
public class HOSClientFactory {

    private static Map<String, HOSClientService> clientCache = new ConcurrentHashMap<>();

    public static HOSClientService getOrCreateHOSClient(String endPoint, String token) {
        String key = endPoint + "_" + token;
        // 判断clientCache是否含有
        if (clientCache.containsKey(key)) {
            // 包含
            return clientCache.get(key);
        }
        HOSClientService client = new HOSClientServiceImpl(endPoint, token);
        clientCache.put(key, client);
        return client;
    }
}
