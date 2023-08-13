package org;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.test1.ZTest1Config;
import org.test1.bean1.A1;

public class Main {
	public static void main(String[] args) {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ZTest1Config.class);
		A1 a1 = (A1)context.getBean("a1");
		a1.ga1();
	}
}
