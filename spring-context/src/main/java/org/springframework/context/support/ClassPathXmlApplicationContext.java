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

package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 独立的 XML 应用上下文，从类路径获取上下文定义文件，将普通路径解释为包含包路径的类路径资源名称
 * （例如 "mypackage/myresource.txt"）。既适用于测试装置，也适用于嵌入在 JAR 中的应用上下文。
 *
 * <p>默认配置位置可通过 {@link #getConfigLocations} 覆盖。配置位置既可以是具体文件，如
 * "/myfiles/context.xml"，也可以是 Ant 风格的匹配模式，如 "/myfiles/*-context.xml"（模式详情请参阅
 * {@link org.springframework.util.AntPathMatcher} 的 javadoc）。
 *
 * <p>注意：存在多个配置位置时，后加载文件中的 Bean 定义会覆盖先加载文件中的定义。可以利用这一点，
 * 通过额外的 XML 文件故意覆盖某些 Bean 定义。
 *
 * <p><b>这是一个简单的一站式便捷 ApplicationContext。如需更灵活的上下文配置，可考虑将
 * {@link GenericApplicationContext} 类与
 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader} 结合使用。</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getResource
 * @see #getResourceByPath
 * @see GenericApplicationContext
 */
public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {

	@Nullable
	private Resource[] configResources;


	/**
	 * 为 Bean 风格的配置创建一个新的 ClassPathXmlApplicationContext。
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public ClassPathXmlApplicationContext() {
	}

	/**
	 * 为 Bean 风格的配置创建一个新的 ClassPathXmlApplicationContext。
	 * @param parent 父上下文
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public ClassPathXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}

	/**
	 * 创建一个新的 ClassPathXmlApplicationContext，从给定的 XML 文件加载定义并自动刷新上下文。
	 * @param configLocation 资源位置
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
		this(new String[] {configLocation}, true, null);
	}

	/**
	 * 创建一个新的 ClassPathXmlApplicationContext，从给定的 XML 文件加载定义并自动刷新上下文。
	 * @param configLocations 资源位置数组
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String... configLocations) throws BeansException {
		this(configLocations, true, null);
	}

	/**
	 * 使用给定的父上下文创建一个新的 ClassPathXmlApplicationContext，从给定的 XML 文件加载定义并自动刷
	 * 新上下文。
	 * @param configLocations 资源位置数组
	 * @param parent 父上下文
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, @Nullable ApplicationContext parent)
			throws BeansException {

		this(configLocations, true, parent);
	}

	/**
	 * 创建一个新的 ClassPathXmlApplicationContext，从给定的 XML 文件加载定义。
	 * @param configLocations 资源位置数组
	 * @param refresh 是否自动刷新上下文，加载所有 Bean 定义并创建所有单例。
	 * 或者在进一步配置上下文后手动调用 refresh。
	 * @throws BeansException 如果上下文创建失败
	 * @see #refresh()
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
		this(configLocations, refresh, null);
	}

	/**
	 * 使用给定的父上下文创建一个新的 ClassPathXmlApplicationContext，从给定的 XML 文件加载定义。
	 * @param configLocations 资源位置数组
	 * @param refresh 是否自动刷新上下文，加载所有 Bean 定义并创建所有单例。
	 * 或者在进一步配置上下文后手动调用 refresh。
	 * @param parent 父上下文
	 * @throws BeansException 如果上下文创建失败
	 * @see #refresh()
	 */
	public ClassPathXmlApplicationContext(
			String[] configLocations, boolean refresh, @Nullable ApplicationContext parent)
			throws BeansException {

		super(parent);
		setConfigLocations(configLocations);
		if (refresh) {
			refresh();
		}
	}


	/**
	 * 创建一个新的 ClassPathXmlApplicationContext，从给定的 XML 文件加载定义并自动刷新上下文。<p>这是
	 * 一个便捷方法，用于加载相对于给定 Class 的类路径资源。如需完全灵活，可考虑使用带
	 * XmlBeanDefinitionReader 和 ClassPathResource 参数的 GenericApplicationContext。
	 * @param path 类路径内的相对（或绝对）路径
	 * @param clazz 用于加载资源的类（作为给定路径的基准）
	 * @throws BeansException 如果上下文创建失败
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String path, Class<?> clazz) throws BeansException {
		this(new String[] {path}, clazz);
	}

	/**
	 * 创建一个新的 ClassPathXmlApplicationContext，从给定的 XML 文件加载定义并自动刷新上下文。
	 * @param paths 类路径内的相对（或绝对）路径数组
	 * @param clazz 用于加载资源的类（作为给定路径的基准）
	 * @throws BeansException 如果上下文创建失败
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz) throws BeansException {
		this(paths, clazz, null);
	}

	/**
	 * 使用给定的父上下文创建一个新的 ClassPathXmlApplicationContext，从给定的 XML 文件加载定义并自动刷
	 * 新上下文。
	 * @param paths 类路径内的相对（或绝对）路径数组
	 * @param clazz 用于加载资源的类（作为给定路径的基准）
	 * @param parent 父上下文
	 * @throws BeansException 如果上下文创建失败
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz, @Nullable ApplicationContext parent)
			throws BeansException {

		super(parent);
		Assert.notNull(paths, "Path array must not be null");
		Assert.notNull(clazz, "Class argument must not be null");
		this.configResources = new Resource[paths.length];
		for (int i = 0; i < paths.length; i++) {
			this.configResources[i] = new ClassPathResource(paths[i], clazz);
		}
		refresh();
	}


	@Override
	@Nullable
	protected Resource[] getConfigResources() {
		return this.configResources;
	}

}
