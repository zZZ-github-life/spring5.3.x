/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mock.jndi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import org.springframework.jndi.JndiTemplate;

/**
 * JndiTemplate 类的简单扩展，始终返回给定对象。
 *
 * <p>对测试非常有用。实际上是一个 mock 对象。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @deprecated Deprecated as of Spring Framework 5.2 in favor of complete solutions from
 * third parties such as <a href="https://github.com/h-thurow/Simple-JNDI">Simple-JNDI</a>
 */
@Deprecated
public class ExpectedLookupTemplate extends JndiTemplate {

	private final Map<String, Object> jndiObjects = new ConcurrentHashMap<>(16);


	/**
	 * 构造一个新的 JndiTemplate，它将针对给定名称始终返回给定对象。
	 * 通过 {@code addObject} 调用填充。
	 * @see #addObject(String, Object)
	 */
	public ExpectedLookupTemplate() {
	}

	/**
	 * 构造一个新的 JndiTemplate，它将始终返回给定对象，
	 * 但仅响应对给定名称的请求。
	 * @param name the name the client is expected to look up
	 * @param object the object that will be returned
	 */
	public ExpectedLookupTemplate(String name, Object object) {
		addObject(name, object);
	}


	/**
	 * 将给定对象添加到此模板将暴露的 JNDI 对象列表中。
	 * @param name the name the client is expected to look up
	 * @param object the object that will be returned
	 */
	public void addObject(String name, Object object) {
		this.jndiObjects.put(name, object);
	}

	/**
	 * 如果名称是构造函数中指定的预期名称，则返回构造函数中
	 * 提供的对象。如果名称不是预期的，则抛出
	 * 相应的 NamingException。
	 */
	@Override
	public Object lookup(String name) throws NamingException {
		Object object = this.jndiObjects.get(name);
		if (object == null) {
			throw new NamingException("Unexpected JNDI name '" + name + "': expecting " + this.jndiObjects.keySet());
		}
		return object;
	}

}
