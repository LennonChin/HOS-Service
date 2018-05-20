package com.coderap.hos.mybatis;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;

/**
 * @program: HOS-Service
 * @description: 数据库连接池类
 * @author: Lennon Chin
 * @create: 2018/05/20 22:25:13
 */
public class HikariDataSourceFactory extends UnpooledDataSourceFactory {

    public HikariDataSourceFactory() {
        this.dataSource = new HikariDataSource();
    }
}
