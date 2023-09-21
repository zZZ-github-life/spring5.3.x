package org.test1;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.Order;

/**
 * 注解配置类
 */
@Order(2)
@ComponentScan({"org.test1.bean1","org.test1.aop"})
@Configuration
@EnableAspectJAutoProxy
public class ZTest1Config {

}
