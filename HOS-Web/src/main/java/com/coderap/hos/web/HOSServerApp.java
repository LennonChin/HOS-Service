package com.coderap.hos.web;

import com.coderap.hos.mybatis.HOSDataSourceConfig;
import com.coderap.hos.web.security.SecurityInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.*;

/**
 * @program: HOS-Service
 * @description: 运行类
 * @author: Lennon Chin
 * @create: 2018/05/23 21:01:48
 */
@EnableWebMvc
@SuppressWarnings("deprecation")
@EnableAutoConfiguration(exclude = MongoAutoConfiguration.class)
@Configuration
@ComponentScan({"com.coderap.hos.*"})
@Import({HOSDataSourceConfig.class, HOSServiceBeanConfiguration.class})
@MapperScan("com.coderap.hos")
@SpringBootApplication
public class HOSServerApp {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SecurityInterceptor securityInterceptor;

    public static void main(String [] args) {
        SpringApplication.run(HOSServerApp.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
//                super.addInterceptors(registry);
                registry.addInterceptor(securityInterceptor);
            }

            @Override
            public void addCorsMappings(CorsRegistry registry) {
//                super.addCorsMappings(registry);
                registry.addMapping("/**").allowedOrigins("*");
            }
        };
    }
}
