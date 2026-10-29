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

package org.springframework.jms.support.destination;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于 Spring {@link BeanFactory} 的 {@link DestinationResolver} 实现。
 *
 * <p>将按 bean 名称查找 Spring 管理的 bean，
 * 并要求其类型为 {@code javax.jms.Destination}。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.BeanFactory
 */
public class BeanFactoryDestinationResolver implements DestinationResolver, BeanFactoryAware {

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 创建一个新的 {@link BeanFactoryDestinationResolver} 类实例。
	 * <p>必须通过 {@code setBeanFactory} 设置要访问的 BeanFactory。
	 * @see #setBeanFactory
	 */
	public BeanFactoryDestinationResolver() {
	}

	/**
	 * 创建一个新的 {@link BeanFactoryDestinationResolver} 类实例。
	 * <p>如果此对象由 Spring IoC 容器创建，则使用此构造函数是多余的，
	 * 因为所提供的 {@link BeanFactory} 会被创建它的 {@link BeanFactory} 替换
	 * （参见 {@link BeanFactoryAware} 契约）。因此，只有在 Spring IoC 容器之外的
	 * 上下文中使用此类时才应使用此构造函数。
	 * @param beanFactory the bean factory to be used to lookup {@link javax.jms.Destination Destination}
	 */
	public BeanFactoryDestinationResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory is required");
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public Destination resolveDestinationName(@Nullable Session session, String destinationName, boolean pubSubDomain)
			throws JMSException {

		Assert.state(this.beanFactory != null, "BeanFactory is required");
		try {
			return this.beanFactory.getBean(destinationName, Destination.class);
		}
		catch (BeansException ex) {
			throw new DestinationResolutionException(
					"Failed to look up Destination bean with name '" + destinationName + "'", ex);
		}
	}

}
