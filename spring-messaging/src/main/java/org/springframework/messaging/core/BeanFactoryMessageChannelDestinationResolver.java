/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.core;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * 一个 {@link DestinationResolver} 实现，它将目标名称解释为 {@link MessageChannel}
 * 的 Bean 名称，并在已配置的 {@link BeanFactory} 中查找该 Bean。
 *
 * @author Mark Fisher
 * @since 4.0
 */
public class BeanFactoryMessageChannelDestinationResolver
		implements DestinationResolver<MessageChannel>, BeanFactoryAware {

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 默认构造函数，可在解析器本身配置为 Spring Bean 时使用；
	 * 由于实现了 {@link BeanFactoryAware}，将注入 {@code BeanFactory}。
	 */
	public BeanFactoryMessageChannelDestinationResolver() {
	}

	/**
	 * 接受 {@link BeanFactory} 的构造函数，适用于手动实例化此解析器
	 * 而不是将其定义为 Spring 管理的 Bean 的情况。
	 * @param beanFactory 用于执行查找的 Bean 工厂
	 */
	public BeanFactoryMessageChannelDestinationResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "beanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public MessageChannel resolveDestination(String name) {
		Assert.state(this.beanFactory != null, "No BeanFactory configured");
		try {
			return this.beanFactory.getBean(name, MessageChannel.class);
		}
		catch (BeansException ex) {
			throw new DestinationResolutionException(
					"Failed to find MessageChannel bean with name '" + name + "'", ex);
		}
	}

}
