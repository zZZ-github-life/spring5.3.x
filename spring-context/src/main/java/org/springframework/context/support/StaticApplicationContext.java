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

package org.springframework.context.support;

import java.util.Locale;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.context.ApplicationContext} 的实现，支持以编程方式注册 Bean 和消息，
 * 而不是从外部配置源读取 Bean 定义。主要用于测试。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #registerSingleton
 * @see #registerPrototype
 * @see #registerBeanDefinition
 * @see #refresh
 */
public class StaticApplicationContext extends GenericApplicationContext {

	private final StaticMessageSource staticMessageSource;


	/**
	 * 创建一个新的 StaticApplicationContext。
	 * @see #registerSingleton
	 * @see #registerPrototype
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public StaticApplicationContext() throws BeansException {
		this(null);
	}

	/**
	 * 使用给定的父上下文创建一个新的 StaticApplicationContext。
	 * @see #registerSingleton
	 * @see #registerPrototype
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public StaticApplicationContext(@Nullable ApplicationContext parent) throws BeansException {
		super(parent);

		// Initialize and register a StaticMessageSource.
		this.staticMessageSource = new StaticMessageSource();
		getBeanFactory().registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.staticMessageSource);
	}


	/**
	 * 重写为无操作，以便对测试用例更加宽容。
	 */
	@Override
	protected void assertBeanFactoryActive() {
	}

	/**
	 * 返回此上下文使用的内部 StaticMessageSource。可用它在其中注册消息。
	 * @see #addMessage
	 */
	public final StaticMessageSource getStaticMessageSource() {
		return this.staticMessageSource;
	}

	/**
	 * 向底层 Bean 工厂注册一个单例 Bean。
	 * <p>对于更高级的需求，请直接向底层 BeanFactory 注册。
	 * @see #getDefaultListableBeanFactory
	 */
	public void registerSingleton(String name, Class<?> clazz) throws BeansException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(clazz);
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * 向底层 Bean 工厂注册一个单例 Bean。
	 * <p>对于更高级的需求，请直接向底层 BeanFactory 注册。
	 * @see #getDefaultListableBeanFactory
	 */
	public void registerSingleton(String name, Class<?> clazz, MutablePropertyValues pvs) throws BeansException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(clazz);
		bd.setPropertyValues(pvs);
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * 向底层 Bean 工厂注册一个原型 Bean。
	 * <p>对于更高级的需求，请直接向底层 BeanFactory 注册。
	 * @see #getDefaultListableBeanFactory
	 */
	public void registerPrototype(String name, Class<?> clazz) throws BeansException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bd.setBeanClass(clazz);
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * 向底层 Bean 工厂注册一个原型 Bean。
	 * <p>对于更高级的需求，请直接向底层 BeanFactory 注册。
	 * @see #getDefaultListableBeanFactory
	 */
	public void registerPrototype(String name, Class<?> clazz, MutablePropertyValues pvs) throws BeansException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bd.setBeanClass(clazz);
		bd.setPropertyValues(pvs);
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * 将给定消息与给定代码关联。
	 * @param code 查找代码
	 * @param locale 应在其内找到消息的 Locale
	 * @param defaultMessage 与此查找代码关联的消息
	 * @see #getStaticMessageSource
	 */
	public void addMessage(String code, Locale locale, String defaultMessage) {
		getStaticMessageSource().addMessage(code, locale, defaultMessage);
	}

}
