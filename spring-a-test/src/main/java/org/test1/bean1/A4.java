package org.test1.bean1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author zZZ.... <br/>
 * @description <br/>
 * @date 2023/10/2$  <br/>
 */
@Component("a4")
public class A4 {

	@Autowired
	private A2 a2;

	@Autowired
	private A3 a3;

	public A4() {
	}

	public A4(A2 a2) {
		this.a2 = a2;
	}


}
