/*
 * Copyright 2002-2013 the original author or authors.
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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

import org.springframework.lang.Nullable;

/**
 * 用于为 Spring Bean 创建 {@link BeanInfo} 实例的策略接口。可用于插入自定义的 Bean 属性解析策略
 * （例如针对 JVM 上的其他语言）或更高效的 {@link BeanInfo} 获取算法。
 *
 * <p>BeanInfoFactory 由 {@link CachedIntrospectionResults} 使用
 * {@link org.springframework.core.io.support.SpringFactoriesLoader} 工具类来实例化。
 *
 * 当需要创建 {@link BeanInfo} 时，{@code CachedIntrospectionResults} 会遍历已发现的工厂，并对每个
 * 工厂调用 {@link #getBeanInfo(Class)}。如果返回 {@code null}，则查询下一个工厂。如果没有任何工厂
 * 支持该类，则将创建一个标准的 {@link BeanInfo} 作为默认值。
 *
 * <p>请注意，{@link org.springframework.core.io.support.SpringFactoriesLoader} 会按
 * {@link org.springframework.core.annotation.Order @Order} 对 {@code BeanInfoFactory} 实例排序，因
 * 此优先级更高的会排在前面。
 *
 * @author Arjen Poutsma
 * @since 3.2
 * @see CachedIntrospectionResults
 * @see org.springframework.core.io.support.SpringFactoriesLoader
 */
public interface BeanInfoFactory {

	/**
	 * 如果支持，返回给定类的 Bean 信息。
	 * @param beanClass the bean class
	 * @return the BeanInfo, or {@code null} if the given class is not supported
	 * @throws IntrospectionException in case of exceptions
	 */
	@Nullable
	BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException;

}
