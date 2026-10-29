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

package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.aop.aspectj.AspectInstanceFactory} 实现，由 Spring
 * {@link org.springframework.beans.factory.BeanFactory} 提供支持。
 *
 * <p>请注意，如果使用 prototype 作用域，这可能会实例化多次，这大概不会产生你所期望的语义。可使用
 * {@link LazySingletonAspectInstanceFactoryDecorator} 包装它，以确保只返回一个新的切面。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.BeanFactory
 * @see LazySingletonAspectInstanceFactoryDecorator
 */
@SuppressWarnings("serial")
public class BeanFactoryAspectInstanceFactory implements MetadataAwareAspectInstanceFactory, Serializable {

	private final BeanFactory beanFactory;

	private final String name;

	private final AspectMetadata aspectMetadata;


	/**
	 * 创建一个 BeanFactoryAspectInstanceFactory。将调用 AspectJ 进行内省，使用 BeanFactory 中给定 Bean
	 * 名称所返回的类型创建 AJType 元数据。
	 * @param beanFactory the BeanFactory to obtain instance(s) from
	 * @param name the name of the bean
	 */
	public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name) {
		this(beanFactory, name, null);
	}

	/**
	 * 创建一个 BeanFactoryAspectInstanceFactory，并提供 AspectJ 应用于内省以创建 AJType 元数据的类型。
	 * 当 BeanFactory 可能将该类型视为子类（例如使用 CGLIB 时），而相关信息应关联到超类时使用。
	 * @param beanFactory the BeanFactory to obtain instance(s) from
	 * @param name the name of the bean
	 * @param type the type that should be introspected by AspectJ
	 * ({@code null} indicates resolution through {@link BeanFactory#getType} via the bean name)
	 */
	public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name, @Nullable Class<?> type) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.notNull(name, "Bean name must not be null");
		this.beanFactory = beanFactory;
		this.name = name;
		Class<?> resolvedType = type;
		if (type == null) {
			resolvedType = beanFactory.getType(name);
			Assert.notNull(resolvedType, "Unresolvable bean type - explicitly specify the aspect class");
		}
		this.aspectMetadata = new AspectMetadata(resolvedType, name);
	}


	@Override
	public Object getAspectInstance() {
		return this.beanFactory.getBean(this.name);
	}

	@Override
	@Nullable
	public ClassLoader getAspectClassLoader() {
		return (this.beanFactory instanceof ConfigurableBeanFactory ?
				((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader() :
				ClassUtils.getDefaultClassLoader());
	}

	@Override
	public AspectMetadata getAspectMetadata() {
		return this.aspectMetadata;
	}

	@Override
	@Nullable
	public Object getAspectCreationMutex() {
		if (this.beanFactory.isSingleton(this.name)) {
			// Rely on singleton semantics provided by the factory -> no local lock.
			return null;
		}
		else if (this.beanFactory instanceof ConfigurableBeanFactory) {
			// No singleton guarantees from the factory -> let's lock locally but
			// reuse the factory's singleton lock, just in case a lazy dependency
			// of our advice bean happens to trigger the singleton lock implicitly...
			return ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
		}
		else {
			return this;
		}
	}

	/**
	 * 确定此工厂目标切面的顺序：或者通过实现 {@link org.springframework.core.Ordered} 接口表达的实例级
	 * 顺序（仅对单例 Bean 检查），或者通过类级别上的 {@link org.springframework.core.annotation.Order}
	 * 注解表达的顺序。
	 * @see org.springframework.core.Ordered
	 * @see org.springframework.core.annotation.Order
	 */
	@Override
	public int getOrder() {
		Class<?> type = this.beanFactory.getType(this.name);
		if (type != null) {
			if (Ordered.class.isAssignableFrom(type) && this.beanFactory.isSingleton(this.name)) {
				return ((Ordered) this.beanFactory.getBean(this.name)).getOrder();
			}
			return OrderUtils.getOrder(type, Ordered.LOWEST_PRECEDENCE);
		}
		return Ordered.LOWEST_PRECEDENCE;
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + ": bean name '" + this.name + "'";
	}

}
