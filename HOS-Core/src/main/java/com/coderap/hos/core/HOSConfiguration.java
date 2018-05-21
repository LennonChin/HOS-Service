package com.coderap.hos.core;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.Properties;

/**
 * @program: HOS-Service
 * @description:
 * @author: Lennon Chin
 * @create: 2018/05/20 23:00:33
 */
public class HOSConfiguration {

    private static HOSConfiguration configuration;
    private static Properties properties;

    // 获取classpath下所有的配置
    static {
        PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        configuration = new HOSConfiguration();
        try {
            configuration.properties = new Properties();
            // 获取所有的properties文件
            Resource[] resources = resourcePatternResolver.getResources("classpath:*.properties"); resourcePatternResolver.getResources("classpath:*.properties");
            // 遍历获取的properties文件
            for (Resource resource : resources) {
                Properties prop = new Properties();
                InputStream inputStream = resource.getInputStream();
                prop.load(inputStream);
                inputStream.close();
                // 将获得的配置添加到configuration的properties中
                configuration.properties.putAll(prop);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HOSConfiguration() {}

    public static HOSConfiguration getConfiguration() {
        return configuration;
    }

    public String getString(String key) {
        return properties.get(key).toString();
    }

    public int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
}
