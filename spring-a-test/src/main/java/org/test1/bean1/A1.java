package org.test1.bean1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import javax.annotation.Resource;
import java.beans.ConstructorProperties;
import java.lang.reflect.Constructor;

@Component("a1")
public class A1 {

	@Autowired
	private A2 a2;


	@Resource
	private A3 a3;

	@Value("ssss")
	private String s;


	public A1(){
		System.out.println("1");

	}


	@Autowired(required = false)
	public A1(A2 a2) {
		this.a2 = a2;
		System.out.println("2");
	}
	@Autowired(required = false)
	public A1(A3 a3) {
		this.a3 = a3;
		System.out.println("3");

	}

	public A1(String s) {
		this.s = s;
		System.out.println("4");

	}
	@Autowired(required = false)
	public A1(A2 a2, A3 a3) {
		this.a2 = a2;
		this.a3 = a3;
		System.out.println("5");

	}

	@Autowired(required = false)
	private A1(A2 a2,  String s) {
		this.a2 = a2;
		this.s = s;
		System.out.println("6");

	}
	@Autowired(required = false)
	public A1(@Lazy @Autowired A3 a3, @Value("2") String s) {
		this.a3 = a3;
		this.s = s;
		System.out.println("7");

	}
	@ConstructorProperties({"a2","a3","s"})
	@Autowired(required = false)
	public A1(A2 a2,  A3 a3,  String s) {
		this.a2 = a2;
		this.a3 = a3;
		this.s = s;
		System.out.println("1");

	}

	public void ga1(){
		System.out.println("正在打印a2："+a2);
	}


}
