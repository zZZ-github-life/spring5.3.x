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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * Bean 定义读取器的简单接口，用于指定使用 {@link Resource} 和 {@link String} 位置参数的加载方法。
 *
 * <p>具体的 Bean 定义读取器当然可以针对其 Bean 定义格式添加额外的 Bean 定义加载和注册方法。
 *
 * <p>请注意，Bean 定义读取器不必实现此接口。它只是为想要遵循标准命名约定的 Bean 定义读取器提供的建
 * 议。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.core.io.Resource
 */
public interface BeanDefinitionReader {

	/**
	 * 返回用于注册 Bean 定义的 Bean 工厂。<p>该工厂以 {@link BeanDefinitionRegistry} 接口的形式暴露，
	 * 封装了与 Bean 定义处理相关的方法。
	 */
	BeanDefinitionRegistry getRegistry();

	/**
	 * 返回用于资源位置的 {@link ResourceLoader}。<p>可以检查其是否实现 {@code ResourcePatternResolver}
	 * 接口并相应地转换，以便为给定资源模式加载多个资源。<p>返回 {@code null} 表示此 Bean 定义读取器无
	 * 法进行绝对资源加载。<p>这主要用于从 Bean 定义资源内部导入更多资源，例如通过 XML Bean 定义中的
	 * “import”标签。不过，建议将此类导入应用于定义资源相对的位置；只有显式的完整资源位置才会触发基于绝
	 * 对路径的资源加载。<p>此外还提供了 {@code loadBeanDefinitions(String)} 方法，用于从资源位置（或位
	 * 置模式）加载 Bean 定义。这是一种便利方式，可避免显式处理 {@code ResourceLoader}。
	 * @see #loadBeanDefinitions(String)
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 */
	@Nullable
	ResourceLoader getResourceLoader();

	/**
	 * 返回用于 Bean 类的类加载器。<p>{@code null} 表示不急切加载 Bean 类，而只是注册带类名的 Bean 定义，
	 * 相应的类将在以后（或永不）解析。
	 */
	@Nullable
	ClassLoader getBeanClassLoader();

	/**
	 * 返回用于匿名 Bean（未指定显式 Bean 名称）的 {@link BeanNameGenerator}。
	 */
	BeanNameGenerator getBeanNameGenerator();


	/**
	 * 从指定资源加载 Bean 定义。
	 * @param resource the resource descriptor
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException;

	/**
	 * 从指定的多个资源加载 Bean 定义。
	 * @param resources the resource descriptors
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException;

	/**
	 * 从指定资源位置加载 Bean 定义。<p>该位置也可以是位置模式，前提是此 Bean 定义读取器的
	 * {@link ResourceLoader} 是 {@code ResourcePatternResolver}。
	 * @param location the resource location, to be loaded with the {@code ResourceLoader}
	 * (or {@code ResourcePatternResolver}) of this bean definition reader
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 * @see #getResourceLoader()
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource)
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource[])
	 */
	int loadBeanDefinitions(String location) throws BeanDefinitionStoreException;

	/**
	 * 从指定的多个资源位置加载 Bean 定义。
	 * @param locations the resource locations, to be loaded with the {@code ResourceLoader}
	 * (or {@code ResourcePatternResolver}) of this bean definition reader
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException;

}
