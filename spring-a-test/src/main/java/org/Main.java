package org;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.test1.ZTest1Config;
import org.test1.bean1.A1;
import org.test1.bean1.A2;
import org.test1.bean1.A3;

public class Main {
	public static void main(String[] args) {
		//ZTest1Config配置类
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ZTest1Config.class);
		A2 a2 =(A2) context.getBean("a2");
		A3 a3 =(A3) context.getBean("a3");

	}
}
