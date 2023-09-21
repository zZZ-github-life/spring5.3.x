package org.test1.aop;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author zZZ.... <br/>
 * @description <br/>
 * @date 2023/9/20$  <br/>
 */
@Aspect
@Component
public class AopProint1 {

	@Pointcut("execution(* org.test1.bean1.*.*(..))")
	public void a(){

	}

	@Before("a()")
	public void b(){
		System.out.println("aop前置通知");
	}

	@After("a()")
	public void c(){
		System.out.println("aop后置通知");
	}
}
