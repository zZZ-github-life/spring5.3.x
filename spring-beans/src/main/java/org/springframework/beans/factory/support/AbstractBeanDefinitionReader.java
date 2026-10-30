/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 实现 {@link BeanDefinitionReader} 接口的 Bean 定义读取器的抽象基类。
 *
 * <p>提供常用属性，例如要操作的 Bean 工厂，以及用于加载 Bean 类的类加载器。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 11.12.2003
 * @see BeanDefinitionReaderUtils
 */
public abstract class AbstractBeanDefinitionReader implements BeanDefinitionReader, EnvironmentCapable {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	private final BeanDefinitionRegistry registry;

	@Nullable
	private ResourceLoader resourceLoader;

	@Nullable
	private ClassLoader beanClassLoader;

	private Environment environment;

	private BeanNameGenerator beanNameGenerator = DefaultBeanNameGenerator.INSTANCE;


	/**
	 * 为给定的 Bean 工厂创建一个新的 AbstractBeanDefinitionReader。<p>如果传入的 Bean 工厂不仅实现了
	 * BeanDefinitionRegistry 接口，还实现了 ResourceLoader 接口，那么它也会被用作默认的 ResourceLoader。
	 * {@link org.springframework.context.ApplicationContext} 实现通常就是这种情况。<p>如果给定的是普
	 * 通的 BeanDefinitionRegistry，则默认的 ResourceLoader 将是
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}。<p>如果传入的
	 * Bean 工厂还实现了 {@link EnvironmentCapable}，此读取器将使用其环境。否则，读取器将初始化并使用
	 * {@link StandardEnvironment}。所有 ApplicationContext 实现都是 EnvironmentCapable，而普通的
	 * BeanFactory 实现则不是。
	 * @param registry 以 BeanDefinitionRegistry 形式加载 Bean 定义的 BeanFactory
	 * @see #setResourceLoader
	 * @see #setEnvironment
	 */
	protected AbstractBeanDefinitionReader(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		this.registry = registry;

		// Determine ResourceLoader to use.
		if (this.registry instanceof ResourceLoader) {
			this.resourceLoader = (ResourceLoader) this.registry;
		}
		else {
			this.resourceLoader = new PathMatchingResourcePatternResolver();
		}

		// Inherit Environment if possible
		if (this.registry instanceof EnvironmentCapable) {
			this.environment = ((EnvironmentCapable) this.registry).getEnvironment();
		}
		else {
			this.environment = new StandardEnvironment();
		}
	}


	/**
	 * 返回用于注册 Bean 定义的 Bean 工厂。<p>该工厂通过 BeanDefinitionRegistry 接口暴露，封装了与 Bean
	 * 定义处理相关的方法。
	 * @deprecated as of Spring Framework 5.3.15 in favor of {@link #getRegistry()},
	 * to be removed in Spring Framework 6.0
	 */
	@Deprecated
	public final BeanDefinitionRegistry getBeanFactory() {
		return this.registry;
	}

	@Override
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * 设置用于资源位置的 ResourceLoader。如果指定的是 ResourcePatternResolver，则 Bean 定义读取器将能
	 * 够把资源模式解析为 Resource 数组。<p>默认是 PathMatchingResourcePatternResolver，它也可以通过
	 * ResourcePatternResolver 接口进行资源模式解析。<p>将其设置为 {@code null} 表示此 Bean 定义读取器
	 * 无法进行绝对资源加载。
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	@Nullable
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * 设置用于 Bean 类的 ClassLoader。<p>默认值为 {@code null}，表示不预先加载 Bean 类，而只是注册带有
	 * 类名的 Bean 定义，相应的 Class 将在稍后（或永远不）解析。
	 * @see Thread#getContextClassLoader()
	 */
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	@Override
	@Nullable
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	/**
	 * 设置读取 Bean 定义时要使用的 Environment。最常用于评估 profile 信息，以确定应读取哪些 Bean 定义、
	 * 应省略哪些 Bean 定义。
	 */
	public void setEnvironment(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public Environment getEnvironment() {
		return this.environment;
	}

	/**
	 * 设置用于匿名 Bean（未指定显式 Bean 名称）的 BeanNameGenerator。<p>默认是
	 * {@link DefaultBeanNameGenerator}。
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : DefaultBeanNameGenerator.INSTANCE);
	}

	@Override
	public BeanNameGenerator getBeanNameGenerator() {
		return this.beanNameGenerator;
	}


	@Override
	public int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException {
		Assert.notNull(resources, "Resource array must not be null");
		int count = 0;
		for (Resource resource : resources) {
			count += loadBeanDefinitions(resource);
		}
		return count;
	}

	@Override
	public int loadBeanDefinitions(String location) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(location, null);
	}

	/**
	 * 从指定的资源位置加载 Bean 定义。<p>该位置也可以是位置模式，前提是此 Bean 定义读取器的
	 * ResourceLoader 是一个 ResourcePatternResolver。
	 * @param location 资源位置，将使用该 Bean 定义读取器的 ResourceLoader
	 * （或 ResourcePatternResolver）加载
	 * @param actualResources 用于填充加载过程中解析出的实际 Resource 对象的 Set。
	 * 可以为 {@code null}，表示调用方对这些 Resource 对象不感兴趣。
	 * @return 找到的 Bean 定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析出错时
	 * @see #getResourceLoader()
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource)
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource[])
	 */
	public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources) throws BeanDefinitionStoreException {
		ResourceLoader resourceLoader = getResourceLoader();
		if (resourceLoader == null) {
			throw new BeanDefinitionStoreException(
					"Cannot load bean definitions from location [" + location + "]: no ResourceLoader available");
		}

		if (resourceLoader instanceof ResourcePatternResolver) {
			// Resource pattern matching available.
			try {
				Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
				int count = loadBeanDefinitions(resources);
				if (actualResources != null) {
					Collections.addAll(actualResources, resources);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Loaded " + count + " bean definitions from location pattern [" + location + "]");
				}
				return count;
			}
			catch (IOException ex) {
				throw new BeanDefinitionStoreException(
						"Could not resolve bean definition resource pattern [" + location + "]", ex);
			}
		}
		else {
			// Can only load single resources by absolute URL.
			Resource resource = resourceLoader.getResource(location);
			int count = loadBeanDefinitions(resource);
			if (actualResources != null) {
				actualResources.add(resource);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Loaded " + count + " bean definitions from location [" + location + "]");
			}
			return count;
		}
	}

	@Override
	public int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException {
		Assert.notNull(locations, "Location array must not be null");
		int count = 0;
		for (String location : locations) {
			count += loadBeanDefinitions(location);
		}
		return count;
	}

}
