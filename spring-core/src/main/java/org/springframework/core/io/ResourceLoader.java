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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * 用于加载资源（例如类路径资源或文件系统资源）的策略接口。需要
 * {@link org.springframework.context.ApplicationContext} 来提供此功能以及扩展的
 * {@link org.springframework.core.io.support.ResourcePatternResolver} 支持。
 *
 * <p>{@link DefaultResourceLoader} 是一个独立实现，可在 ApplicationContext 之外使用，
 * {@link ResourceEditor} 也使用它。
 *
 * <p>在 ApplicationContext 中运行时，类型为 {@code Resource} 和 {@code Resource[]} 的 Bean 属性可
 * 以从 String 填充，使用特定上下文的资源加载策略。
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see Resource
 * @see org.springframework.core.io.support.ResourcePatternResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
public interface ResourceLoader {

	/** 从类路径加载的伪 URL 前缀："classpath:"。 */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


	/**
	 * 返回指定资源位置的 {@code Resource} 句柄。
	 * <p>该句柄应始终是可重用的资源描述符，允许进行多次 {@link Resource#getInputStream()} 调用。
	 * <p><ul>
	 * <li>必须支持完全限定的 URL，例如 "file:C:/test.dat"。
	 * <li>必须支持类路径伪 URL，例如 "classpath:test.dat"。
	 * <li>应支持相对文件路径，例如 "WEB-INF/test.dat"。
	 * （这将是实现特定的，通常由 ApplicationContext 实现提供。）
	 * </ul>
	 * <p>请注意，{@code Resource} 句柄并不意味着资源一定存在；需要调用 {@link Resource#exists} 检查其
	 * 是否存在。
	 * @param location the resource location
	 * @return a corresponding {@code Resource} handle (never {@code null})
	 * @see #CLASSPATH_URL_PREFIX
	 * @see Resource#exists()
	 * @see Resource#getInputStream()
	 */
	Resource getResource(String location);

	/**
	 * 暴露此 {@code ResourceLoader} 使用的 {@link ClassLoader}。
	 * <p>需要直接访问 {@code ClassLoader} 的客户端可以以统一的方式通过 {@code ResourceLoader} 进行访问，
	 * 而无需依赖线程上下文 {@code ClassLoader}。
	 * @return the {@code ClassLoader}
	 * (only {@code null} if even the system {@code ClassLoader} isn't accessible)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getClassLoader();

}
