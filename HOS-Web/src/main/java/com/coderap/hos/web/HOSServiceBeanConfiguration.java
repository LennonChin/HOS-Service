package com.coderap.hos.web;

import com.coderap.hos.core.HOSConfiguration;
import com.coderap.hos.server.service.HOSStoreService;
import com.coderap.hos.server.service.impl.HDFSServiceImpl;
import com.coderap.hos.server.service.impl.HOSStoreServiceImpl;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: HOS-Service
 * @description: 配置类
 * @author: Lennon Chin
 * @create: 2018/05/23 19:54:12
 */
@Configuration
public class HOSServiceBeanConfiguration {

    // 获取HBaseConnection 注入到bean中
    @Bean
    public Connection getConnection() throws Exception {
        org.apache.hadoop.conf.Configuration configuration = HBaseConfiguration.create();
        HOSConfiguration hosConfiguration = HOSConfiguration.getConfiguration();
        configuration.set("hbase.zookeeper.quorum", hosConfiguration.getString("hbase.zookeeper.quorum"));
        configuration.set("hbase.zookeeper.property.clientPort", hosConfiguration.getString("hbase.zookeeper.property.clientPort"));
        return ConnectionFactory.createConnection(configuration);
    }

    // 实例化一个HOSStore的实例
    @Bean
    public HOSStoreService getHOSStoreService(@Autowired Connection connection) throws Exception {
        HOSConfiguration configuration = HOSConfiguration.getConfiguration();
        String zookeeperHosts = configuration.getString("hbase.zookeeper.quorum");
        HOSStoreServiceImpl hosStoreService = new HOSStoreServiceImpl(connection, new HDFSServiceImpl(), zookeeperHosts);
        return hosStoreService;
    }

    @Bean
    public JettyEmbeddedServletContainerFactory servletContainer() {
        JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory();
        return factory;
    }
}
