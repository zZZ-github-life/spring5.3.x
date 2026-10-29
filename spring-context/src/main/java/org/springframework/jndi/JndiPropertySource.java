/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

/**
 * {@link PropertySource} 实现，从底层的 Spring {@link JndiLocatorDelegate} 读取属性。
 *
 * <p>默认情况下，底层 {@code JndiLocatorDelegate} 的
 * {@link JndiLocatorDelegate#setResourceRef(boolean) "resourceRef"} 属性会被设置为 {@code true}，
 * 这意味着所查找的名称会自动加上 "java:comp/env/" 前缀，以符合已发布的
 * <a href="https://download.oracle.com/javase/jndi/tutorial/beyond/misc/policy.html">JNDI 命名约定
 * </a>。要覆盖此设置或更改前缀，请手动配置一个 {@code JndiLocatorDelegate}，并将其提供给这里接受该
 * 参数的某个构造器。提供自定义 JNDI 属性时同样如此。这些属性应在构造 {@code JndiPropertySource} 之
 * 前使用 {@link JndiLocatorDelegate#setJndiEnvironment(java.util.Properties)} 指定。
 *
 * <p>注意，
 * {@link org.springframework.web.context.support.StandardServletEnvironment StandardServletEnvironment}
 * 默认包含一个 {@code JndiPropertySource}，对底层 {@link JndiLocatorDelegate} 的任何定制都可以在
 * {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}
 * 或 {@link org.springframework.web.WebApplicationInitializer WebApplicationInitializer} 中执行。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see JndiLocatorDelegate
 * @see org.springframework.context.ApplicationContextInitializer
 * @see org.springframework.web.WebApplicationInitializer
 * @see org.springframework.web.context.support.StandardServletEnvironment
 */
public class JndiPropertySource extends PropertySource<JndiLocatorDelegate> {

	/**
	 * 使用给定名称创建一个新的 {@code JndiPropertySource}，并使用一个配置为给所有名称加上
	 * "java:comp/env/" 前缀的 {@link JndiLocatorDelegate}。
	 */
	public JndiPropertySource(String name) {
		this(name, JndiLocatorDelegate.createDefaultResourceRefLocator());
	}

	/**
	 * 使用给定名称和给定的 {@code JndiLocatorDelegate} 创建一个新的 {@code JndiPropertySource}。
	 */
	public JndiPropertySource(String name, JndiLocatorDelegate jndiLocator) {
		super(name, jndiLocator);
	}


	/**
	 * 此实现从底层 {@link JndiLocatorDelegate} 查找并返回与给定名称关联的值。如果调用
	 * {@link JndiLocatorDelegate#lookup(String)} 时抛出 {@link NamingException}，则返回 {@code null}，
	 * 并以异常消息发出 DEBUG 级别的日志记录。
	 */
	@Override
	@Nullable
	public Object getProperty(String name) {
		if (getSource().isResourceRef() && name.indexOf(':') != -1) {
			// We're in resource-ref (prefixing with "java:comp/env") mode. Let's not bother
			// with property names with a colon it since they're probably just containing a
			// default value clause, very unlikely to match including the colon part even in
			// a textual property source, and effectively never meant to match that way in
			// JNDI where a colon indicates a separator between JNDI scheme and actual name.
			return null;
		}

		try {
			Object value = this.source.lookup(name);
			if (logger.isDebugEnabled()) {
				logger.debug("JNDI lookup for name [" + name + "] returned: [" + value + "]");
			}
			return value;
		}
		catch (NamingException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("JNDI lookup for name [" + name + "] threw NamingException " +
						"with message: " + ex.getMessage() + ". Returning null.");
			}
			return null;
		}
	}

}
