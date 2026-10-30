/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jndi;

import javax.naming.NamingException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于基于 JNDI 的服务定位器的便捷父类，提供对特定 JNDI 资源的可配置查找。
 *
 * <p>暴露一个 {@link #setJndiName "jndiName"} 属性。它可能包含也可能不包含 Java EE 应用在访问本地
 * 映射（Environmental Naming Context）资源时所期望的 "java:comp/env/" 前缀。如果不包含，那么在
 * "resourceRef" 属性为 true（默认为 <strong>false</strong>）且未给出其他 scheme（例如 "java:"）时，
 * 会自动加上 "java:comp/env/" 前缀。
 *
 * <p>子类可以在适当的时机调用 {@link #lookup()} 方法。有些类可能在初始化时调用，而另一些类可能按需
 * 调用。后一种策略更灵活，因为它允许在 JNDI 对象可用之前就初始化定位器。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJndiName
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setResourceRef
 * @see #lookup()
 */
public abstract class JndiObjectLocator extends JndiLocatorSupport implements InitializingBean {

	@Nullable
	private String jndiName;

	@Nullable
	private Class<?> expectedType;


	/**
	 * 指定要查找的 JNDI 名称。如果它不以 "java:comp/env/" 开头，则在 "resourceRef" 设置为 "true" 时会
	 * 自动添加该前缀。
	 * @param jndiName 要查找的 JNDI 名称
	 * @see #setResourceRef
	 */
	public void setJndiName(@Nullable String jndiName) {
		this.jndiName = jndiName;
	}

	/**
	 * 返回要查找的 JNDI 名称。
	 */
	@Nullable
	public String getJndiName() {
		return this.jndiName;
	}

	/**
	 * 指定所定位的 JNDI 对象应可赋值给的类型（如果有）。
	 */
	public void setExpectedType(@Nullable Class<?> expectedType) {
		this.expectedType = expectedType;
	}

	/**
	 * 返回所定位的 JNDI 对象应可赋值给的类型（如果有）。
	 */
	@Nullable
	public Class<?> getExpectedType() {
		return this.expectedType;
	}

	@Override
	public void afterPropertiesSet() throws IllegalArgumentException, NamingException {
		if (!StringUtils.hasLength(getJndiName())) {
			throw new IllegalArgumentException("Property 'jndiName' is required");
		}
	}


	/**
	 * 执行此定位器目标资源的实际 JNDI 查找。
	 * @return 定位到的目标对象
	 * @throws NamingException 如果 JNDI 查找失败，
	 * 或者定位到的 JNDI 对象不能赋值给预期类型
	 * @see #setJndiName
	 * @see #setExpectedType
	 * @see #lookup(String, Class)
	 */
	protected Object lookup() throws NamingException {
		String jndiName = getJndiName();
		Assert.state(jndiName != null, "No JNDI name specified");
		return lookup(jndiName, getExpectedType());
	}

}
