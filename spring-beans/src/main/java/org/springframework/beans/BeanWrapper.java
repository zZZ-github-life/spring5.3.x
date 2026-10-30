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

package org.springframework.beans;

import java.beans.PropertyDescriptor;

/**
 * Spring 底层 JavaBeans 基础设施的核心接口。
 *
 * <p>通常不直接使用，而是通过 {@link org.springframework.beans.factory.BeanFactory} 或
 * {@link org.springframework.validation.DataBinder} 隐式使用。
 *
 * <p>提供用于分析和操作标准 JavaBeans 的操作：获取和设置属性值（单独或批量）、获取属性描述符，以及
 * 查询属性的可读性/可写性。
 *
 * <p>此接口支持<b>嵌套属性</b>，可以将子属性上的属性设置到无限深度。
 *
 * <p>BeanWrapper 对 "extractOldValueForEditor" 设置的默认值为 "false"，以避免调用 getter 方法带来
 * 的副作用。将其设为 "true"，可将当前属性值暴露给自定义编辑器。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13 April 2001
 * @see PropertyAccessor
 * @see PropertyEditorRegistry
 * @see PropertyAccessorFactory#forBeanPropertyAccess
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.validation.BeanPropertyBindingResult
 * @see org.springframework.validation.DataBinder#initBeanPropertyAccess()
 */
public interface BeanWrapper extends ConfigurablePropertyAccessor {

	/**
	 * 指定数组和集合自动增长的限值。<p>在普通 BeanWrapper 上默认不限制。
	 * @since 4.1
	 */
	void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

	/**
	 * 返回数组和集合自动增长的限值。
	 * @since 4.1
	 */
	int getAutoGrowCollectionLimit();

	/**
	 * 返回此对象包装的 Bean 实例。
	 */
	Object getWrappedInstance();

	/**
	 * 返回被包装 Bean 实例的类型。
	 */
	Class<?> getWrappedClass();

	/**
	 * 获取被包装对象的 PropertyDescriptors（由标准 JavaBeans 内省确定）。
	 * @return 所包装对象的 PropertyDescriptor
	 */
	PropertyDescriptor[] getPropertyDescriptors();

	/**
	 * 获取被包装对象特定属性的属性描述符。
	 * @param propertyName 要获取其描述符的属性
	 * （可以是嵌套路径，但不能是索引/映射属性）
	 * @return 指定属性的属性描述符
	 * @throws InvalidPropertyException 如果不存在这样的属性
	 */
	PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;

}
