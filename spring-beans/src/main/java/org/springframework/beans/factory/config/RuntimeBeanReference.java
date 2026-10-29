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

package org.springframework.beans.factory.config;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 不可变占位符类，用于属性值对象，此时该值为对工厂中另一个 Bean 的引用，将在运行时解析。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see BeanDefinition#getPropertyValues()
 * @see org.springframework.beans.factory.BeanFactory#getBean(String)
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
 */
public class RuntimeBeanReference implements BeanReference {

	private final String beanName;

	@Nullable
	private final Class<?> beanType;

	private final boolean toParent;

	@Nullable
	private Object source;


	/**
	 * 为给定 Bean 名称创建一个新的 RuntimeBeanReference。
	 * @param beanName name of the target bean
	 */
	public RuntimeBeanReference(String beanName) {
		this(beanName, false);
	}

	/**
	 * 为给定 Bean 名称创建一个新的 RuntimeBeanReference，可选择将其标记为对父工厂中 Bean 的引用。
	 * @param beanName name of the target bean
	 * @param toParent whether this is an explicit reference to a bean in the
	 * parent factory
	 */
	public RuntimeBeanReference(String beanName, boolean toParent) {
		Assert.hasText(beanName, "'beanName' must not be empty");
		this.beanName = beanName;
		this.beanType = null;
		this.toParent = toParent;
	}

	/**
	 * 为给定类型的 Bean 创建一个新的 RuntimeBeanReference。
	 * @param beanType type of the target bean
	 * @since 5.2
	 */
	public RuntimeBeanReference(Class<?> beanType) {
		this(beanType, false);
	}

	/**
	 * 为给定类型的 Bean 创建一个新的 RuntimeBeanReference，可选择将其标记为对父工厂中 Bean 的引用。
	 * @param beanType type of the target bean
	 * @param toParent whether this is an explicit reference to a bean in the
	 * parent factory
	 * @since 5.2
	 */
	public RuntimeBeanReference(Class<?> beanType, boolean toParent) {
		Assert.notNull(beanType, "'beanType' must not be empty");
		this.beanName = beanType.getName();
		this.beanType = beanType;
		this.toParent = toParent;
	}


	/**
	 * 返回请求的 Bean 名称，在按类型解析的情况下返回完全限定类型名。
	 * @see #getBeanType()
	 */
	@Override
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 如果要求按类型解析，则返回请求的 Bean 类型。
	 * @since 5.2
	 */
	@Nullable
	public Class<?> getBeanType() {
		return this.beanType;
	}

	/**
	 * 返回这是否为对父工厂中 Bean 的显式引用。
	 */
	public boolean isToParent() {
		return this.toParent;
	}

	/**
	 * 为此元数据元素设置配置来源 {@code Object}。
	 * <p>对象的确切类型取决于所使用的配置机制。
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RuntimeBeanReference)) {
			return false;
		}
		RuntimeBeanReference that = (RuntimeBeanReference) other;
		return (this.beanName.equals(that.beanName) && this.beanType == that.beanType &&
				this.toParent == that.toParent);
	}

	@Override
	public int hashCode() {
		int result = this.beanName.hashCode();
		result = 29 * result + (this.toParent ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return '<' + getBeanName() + '>';
	}

}
