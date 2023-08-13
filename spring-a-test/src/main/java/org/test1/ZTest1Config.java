package org.test1;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Order(2)
@ComponentScan("org.test1.bean1")
@Configuration
public class ZTest1Config {


}
