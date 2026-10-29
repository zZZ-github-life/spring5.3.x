/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;

import org.springframework.beans.factory.BeanFactory;

/**
 * 由 {@link BeanFactory} 提供的 prototype 支持的
 * {@link org.springframework.aop.aspectj.AspectInstanceFactory}，强制实施 prototype 语义。
 *
 * <p>请注意，这可能会实例化多次，这大概不会产生你所期望的语义。可使用
 * {@link LazySingletonAspectInstanceFactoryDecorator} 包装它，以确保只返回一个新的切面。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.BeanFactory
 * @see LazySingletonAspectInstanceFactoryDecorator
 */
@SuppressWarnings("serial")
public class PrototypeAspectInstanceFactory extends BeanFactoryAspectInstanceFactory implements Serializable {

	/**
	 * 创建一个 PrototypeAspectInstanceFactory。将调用 AspectJ 进行内省，使用 BeanFactory 中给定 Bean
	 * 名称所返回的类型创建 AJType 元数据。
	 * @param beanFactory the BeanFactory to obtain instance(s) from
	 * @param name the name of the bean
	 */
	public PrototypeAspectInstanceFactory(BeanFactory beanFactory, String name) {
		super(beanFactory, name);
		if (!beanFactory.isPrototype(name)) {
			throw new IllegalArgumentException(
					"Cannot use PrototypeAspectInstanceFactory with bean named '" + name + "': not a prototype");
		}
	}

}
