package org.test1.bean1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("a2")
public class A2 {

	@Autowired
	private A1 a1;

	public void ga(){
		System.out.println("正在打印a1："+a1);
	}

}
