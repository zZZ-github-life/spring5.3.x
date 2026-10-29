/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.Collection;

import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;

/**
 * 绕过标准 {@link java.beans.Introspector} 以加快内省的 {@link BeanInfoFactory} 实现，其功能缩减为
 * 基本的属性判定（Spring 中通常所需的程度）。
 *
 * <p>可通过内容如下的 {@code META-INF/spring.factories} 文件进行配置，以覆盖其他自定义的
 * {@code org.springframework.beans.BeanInfoFactory} 声明：
 * {@code org.springframework.beans.BeanInfoFactory=org.springframework.beans.SimpleBeanInfoFactory}
 *
 * <p>排序值为 {@code Ordered.LOWEST_PRECEDENCE - 1}，以便在必要时覆盖
 * {@link ExtendedBeanInfoFactory}（5.3 中默认注册），同时仍允许其他用户自定义的
 * {@link BeanInfoFactory} 类型优先。
 *
 * @author Juergen Hoeller
 * @since 5.3.24
 * @see ExtendedBeanInfoFactory
 * @see CachedIntrospectionResults
 */
public class SimpleBeanInfoFactory implements BeanInfoFactory, Ordered {

	@Override
	@NonNull
	public BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
		Collection<? extends PropertyDescriptor> pds =
				PropertyDescriptorUtils.determineBasicProperties(beanClass);

		return new SimpleBeanInfo() {
			@Override
			public BeanDescriptor getBeanDescriptor() {
				return new BeanDescriptor(beanClass);
			}
			@Override
			public PropertyDescriptor[] getPropertyDescriptors() {
				return pds.toArray(PropertyDescriptorUtils.EMPTY_PROPERTY_DESCRIPTOR_ARRAY);
			}
		};
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 1;
	}

}
