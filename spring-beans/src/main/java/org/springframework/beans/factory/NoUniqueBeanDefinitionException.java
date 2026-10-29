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

package org.springframework.beans.factory;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 当向 {@code BeanFactory} 请求某个 Bean 实例，而预期只有一个匹配的 Bean 却找到多个匹配候选时，抛
 * 出该异常。
 *
 * @author Juergen Hoeller
 * @since 3.2.1
 * @see BeanFactory#getBean(Class)
 */
@SuppressWarnings("serial")
public class NoUniqueBeanDefinitionException extends NoSuchBeanDefinitionException {

	private final int numberOfBeansFound;

	@Nullable
	private final Collection<String> beanNamesFound;


	/**
	 * 创建一个新的 {@code NoUniqueBeanDefinitionException}。
	 * @param type required type of the non-unique bean
	 * @param numberOfBeansFound the number of matching beans
	 * @param message detailed message describing the problem
	 */
	public NoUniqueBeanDefinitionException(Class<?> type, int numberOfBeansFound, String message) {
		super(type, message);
		this.numberOfBeansFound = numberOfBeansFound;
		this.beanNamesFound = null;
	}

	/**
	 * 创建一个新的 {@code NoUniqueBeanDefinitionException}。
	 * @param type required type of the non-unique bean
	 * @param beanNamesFound the names of all matching beans (as a Collection)
	 */
	public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> beanNamesFound) {
		super(type, "expected single matching bean but found " + beanNamesFound.size() + ": " +
				StringUtils.collectionToCommaDelimitedString(beanNamesFound));
		this.numberOfBeansFound = beanNamesFound.size();
		this.beanNamesFound = beanNamesFound;
	}

	/**
	 * 创建一个新的 {@code NoUniqueBeanDefinitionException}。
	 * @param type required type of the non-unique bean
	 * @param beanNamesFound the names of all matching beans (as an array)
	 */
	public NoUniqueBeanDefinitionException(Class<?> type, String... beanNamesFound) {
		this(type, Arrays.asList(beanNamesFound));
	}

	/**
	 * 创建一个新的 {@code NoUniqueBeanDefinitionException}。
	 * @param type required type of the non-unique bean
	 * @param beanNamesFound the names of all matching beans (as a Collection)
	 * @since 5.1
	 */
	public NoUniqueBeanDefinitionException(ResolvableType type, Collection<String> beanNamesFound) {
		super(type, "expected single matching bean but found " + beanNamesFound.size() + ": " +
				StringUtils.collectionToCommaDelimitedString(beanNamesFound));
		this.numberOfBeansFound = beanNamesFound.size();
		this.beanNamesFound = beanNamesFound;
	}

	/**
	 * 创建一个新的 {@code NoUniqueBeanDefinitionException}。
	 * @param type required type of the non-unique bean
	 * @param beanNamesFound the names of all matching beans (as an array)
	 * @since 5.1
	 */
	public NoUniqueBeanDefinitionException(ResolvableType type, String... beanNamesFound) {
		this(type, Arrays.asList(beanNamesFound));
	}


	/**
	 * 返回在预期只有一个匹配 Bean 时找到的 Bean 数量。对于 NoUniqueBeanDefinitionException，该值通常大
	 * 于 1。
	 * @see #getBeanType()
	 */
	@Override
	public int getNumberOfBeansFound() {
		return this.numberOfBeansFound;
	}

	/**
	 * 返回在预期只有一个匹配 Bean 时找到的所有 Bean 的名称。注意，如果在构造时未指定，该值可能为
	 * {@code null}。
	 * @since 4.3
	 * @see #getBeanType()
	 */
	@Nullable
	public Collection<String> getBeanNamesFound() {
		return this.beanNamesFound;
	}

}
