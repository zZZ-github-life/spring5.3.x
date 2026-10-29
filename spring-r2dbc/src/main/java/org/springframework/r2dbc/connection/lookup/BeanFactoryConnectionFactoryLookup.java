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

package org.springframework.r2dbc.connection.lookup;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于 Spring {@link BeanFactory} 的 {@link ConnectionFactoryLookup} 实现。
 *
 * <p>将按 bean 名称查找由 Spring 管理的 bean，
 * 并期望它们的类型为 {@link ConnectionFactory}。
 *
 * @author Mark Paluch
 * @since 5.3
 * @see BeanFactory
 */
public class BeanFactoryConnectionFactoryLookup implements ConnectionFactoryLookup, BeanFactoryAware {

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 创建一个新的 {@link BeanFactoryConnectionFactoryLookup} 类实例。
	 * <p>要访问的 BeanFactory 必须通过 {@code setBeanFactory} 设置。
	 * @see #setBeanFactory
	 */
	public BeanFactoryConnectionFactoryLookup() {}

	/**
	 * 创建一个新的 {@link BeanFactoryConnectionFactoryLookup} 类实例。
	 * <p>如果此对象由 Spring IoC 容器创建，则使用此构造函数是多余的，
	 * 因为所提供的 {@link BeanFactory} 会被创建它的 {@link BeanFactory}
	 * 替换（参见 {@link BeanFactoryAware} 约定）。因此，只有在 Spring IoC
	 * 容器之外使用此类时才应使用此构造函数。
	 * @param beanFactory the bean factory to be used to lookup {@link ConnectionFactory
	 * ConnectionFactories}
	 */
	public BeanFactoryConnectionFactoryLookup(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName)
			throws ConnectionFactoryLookupFailureException {

		Assert.state(this.beanFactory != null, "BeanFactory is required");
		try {
			return this.beanFactory.getBean(connectionFactoryName, ConnectionFactory.class);
		}
		catch (BeansException ex) {
			throw new ConnectionFactoryLookupFailureException(
					String.format("Failed to look up ConnectionFactory bean with name '%s'", connectionFactoryName), ex);
		}
	}

}
