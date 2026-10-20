package online.zzzzzzz;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import online.zzzzzzz.test1.ZTest1Config;
import online.zzzzzzz.test1.bean1.A2;
import online.zzzzzzz.test1.bean1.A3;

public class Main {
	public static void main(String[] args) {
		//ZTest1Config配置类
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ZTest1Config.class);
		A2 a2 =(A2) context.getBean("a2");
		A3 a3 =(A3) context.getBean("a3");

	}
}
