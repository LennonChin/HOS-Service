package com.coderap.hos.mybatis;

import org.junit.runner.RunWith;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @program: HOS-Service
 * @description: 测试基类
 * @author: Lennon Chin
 * @create: 2018/05/20 22:40:56
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Import(HOSDataSourceConfig.class)
@PropertySource("classpath:application.properties")
@ComponentScan("com.coderap.hos.*")
@MapperScan("com.coderap.hos.*")
public class BaseTest {
}
