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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.core.SpringProperties;
import org.springframework.lang.Nullable;

/**
 * {@link JndiLocatorSupport} 的子类，带有公开的查找方法，便于作为委托使用。
 *
 * @author Juergen Hoeller
 * @since 3.0.1
 */
public class JndiLocatorDelegate extends JndiLocatorSupport {

	/**
	 * 系统属性，指示 Spring 忽略默认的 JNDI 环境，即始终从
	 * {@link #isDefaultJndiEnvironmentAvailable()} 返回 {@code false}。
	 * <p>默认值为 "false"，允许常规的默认 JNDI 访问，例如在 {@link JndiPropertySource} 中。将此外部标
	 * 志切换为 {@code true} 是一种优化，适用于此类 JNDI 回退搜索本来就什么也找不到的场景，可避免重复的
	 * JNDI 查找开销。
	 * <p>请注意，此标志只影响 JNDI 回退搜索，不影响显式配置的 JNDI 查找，例如针对 {@code DataSource}
	 * 或其他环境资源的查找。该标志实际上只影响基于
	 * {@code JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()} 检查来尝试 JNDI 搜索的代码：特别
	 * 是 {@code StandardServletEnvironment} 和 {@code StandardPortletEnvironment}。
	 * @since 4.3
	 * @see #isDefaultJndiEnvironmentAvailable()
	 * @see JndiPropertySource
	 */
	public static final String IGNORE_JNDI_PROPERTY_NAME = "spring.jndi.ignore";


	private static final boolean shouldIgnoreDefaultJndiEnvironment =
			SpringProperties.getFlag(IGNORE_JNDI_PROPERTY_NAME);


	@Override
	public Object lookup(String jndiName) throws NamingException {
		return super.lookup(jndiName);
	}

	@Override
	public <T> T lookup(String jndiName, @Nullable Class<T> requiredType) throws NamingException {
		return super.lookup(jndiName, requiredType);
	}


	/**
	 * 配置一个 {@code JndiLocatorDelegate}，将其 "resourceRef" 属性设置为 {@code true}，这意味着所有名
	 * 称都会加上 "java:comp/env/" 前缀。
	 * @see #setResourceRef
	 */
	public static JndiLocatorDelegate createDefaultResourceRefLocator() {
		JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();
		jndiLocator.setResourceRef(true);
		return jndiLocator;
	}

	/**
	 * 检查此 JVM 上是否可用默认的 JNDI 环境（如 Java EE 环境中的那样）。
	 * @return {@code true} if a default InitialContext can be used,
	 * {@code false} if not
	 */
	public static boolean isDefaultJndiEnvironmentAvailable() {
		if (shouldIgnoreDefaultJndiEnvironment) {
			return false;
		}
		try {
			new InitialContext().getEnvironment();
			return true;
		}
		catch (Throwable ex) {
			return false;
		}
	}

}
