package com.coderap.hos.mybatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * @program: HOS-Service
 * @description: 配置类
 * @author: Lennon Chin
 * @create: 2018/05/20 22:26:35
 */
@Configuration
@MapperScan(basePackages = HOSDataSourceConfig.PACKAGE, sqlSessionFactoryRef = "HOSSqlSessionFactory")
public class HOSDataSourceConfig {

    static final String PACKAGE = "com.coderap.hos.**";

    /**
     * 获取DataSource
     * @return
     * @throws Exception
     */
    @Bean(name = "hosDataSource")
    @Primary
    public DataSource hosDataSource() throws Exception {

        // 获取DataSource相关的信息
        ResourceLoader loader = new DefaultResourceLoader();
        InputStream inputStream = loader.getResource("classpath:application.properties").getInputStream();
        Properties properties = new Properties();
        properties.load(inputStream);
        Set<Object> keys = properties.keySet();
        Properties dsProperties = new Properties();

        for (Object key : keys) {
            if (key.toString().startsWith("datasource")) {
                dsProperties.put(key.toString().replace("datasource.", ""), properties.get(key));
            }
        }
        // 通过HikariDataSourceFactory生成DataSource
        HikariDataSourceFactory hikariDataSourceFactory = new HikariDataSourceFactory();
        hikariDataSourceFactory.setProperties(dsProperties);
        inputStream.close();
        return hikariDataSourceFactory.getDataSource();
    }

    @Bean(name = "HOSSqlSessionFactory")
    @Primary
    public SqlSessionFactory hosSqlSessionFactory(@Qualifier("hosDataSource") DataSource hosDataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(hosDataSource);

        // 读取Mybatis相关的配置
        ResourceLoader loader = new DefaultResourceLoader();
        sqlSessionFactoryBean.setConfigLocation(loader.getResource("classpath:mybatis-config.xml"));

        // 获取SQLSessionFactory实例
        sqlSessionFactoryBean.setSqlSessionFactoryBuilder(new SqlSessionFactoryBuilder());

        return sqlSessionFactoryBean.getObject();
    }
}
