package org.test1;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Order(1)
@ComponentScan("org.test2")
@Configuration
public class ZTest2Config {


}
