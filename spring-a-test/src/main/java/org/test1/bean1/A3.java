package org.test1.bean1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("a3")
public class A3 {

	@Autowired
	private A2 a2;

	public void s(){
		System.out.println("正在执行a3的s方法");
	}
}
