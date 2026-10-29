/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.scheduling.concurrent;

import java.util.Properties;
import java.util.concurrent.Executor;

import javax.naming.NamingException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiTemplate;
import org.springframework.lang.Nullable;

/**
 * {@link ConcurrentTaskExecutor} 的基于 JNDI 的变体，在 Java EE 7/8 环境中对 JSR-236 的
 * "java:comp/DefaultManagedExecutorService" 执行默认查找。
 *
 * <p>注意：此类并非严格基于 JSR-236；它可以与在 JNDI 中找到的任何普通
 * {@link java.util.concurrent.Executor} 配合工作。真正的适配到
 * {@link javax.enterprise.concurrent.ManagedExecutorService} 的工作发生在基类
 * {@link ConcurrentTaskExecutor} 本身中。
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see javax.enterprise.concurrent.ManagedExecutorService
 */
public class DefaultManagedTaskExecutor extends ConcurrentTaskExecutor implements InitializingBean {

	private final JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();

	@Nullable
	private String jndiName = "java:comp/DefaultManagedExecutorService";


	/**
	 * 设置用于 JNDI 查找的 JNDI 模板。
	 * @see org.springframework.jndi.JndiAccessor#setJndiTemplate
	 */
	public void setJndiTemplate(JndiTemplate jndiTemplate) {
		this.jndiLocator.setJndiTemplate(jndiTemplate);
	}

	/**
	 * 设置用于 JNDI 查找的 JNDI 环境。
	 * @see org.springframework.jndi.JndiAccessor#setJndiEnvironment
	 */
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiLocator.setJndiEnvironment(jndiEnvironment);
	}

	/**
	 * 设置查找是否发生在 Java EE 容器中，即当 JNDI 名称尚未包含前缀 "java:comp/env/" 时是否需要添加该
	 * 前缀。PersistenceAnnotationBeanPostProcessor 的默认值为 "true"。
	 * @see org.springframework.jndi.JndiLocatorSupport#setResourceRef
	 */
	public void setResourceRef(boolean resourceRef) {
		this.jndiLocator.setResourceRef(resourceRef);
	}

	/**
	 * 指定要委托给的 {@link java.util.concurrent.Executor} 的 JNDI 名称，替换默认 JNDI 名称
	 * "java:comp/DefaultManagedExecutorService"。
	 * <p>这可以是完全限定的 JNDI 名称，或者在 "resourceRef" 设置为 "true" 时相对于当前环境命名上下文的
	 * JNDI 名称。
	 * @see #setConcurrentExecutor
	 * @see #setResourceRef
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.jndiName != null) {
			setConcurrentExecutor(this.jndiLocator.lookup(this.jndiName, Executor.class));
		}
	}

}
