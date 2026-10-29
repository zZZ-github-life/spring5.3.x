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

package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.core.io.ResourceLoader;

/**
 * 任何希望获知其运行所在的 {@link ResourceLoader}（通常是 ApplicationContext）的对象都可以实现此接
 * 口。这是通过 {@link org.springframework.context.ApplicationContextAware} 接口引入完整
 * {@link ApplicationContext} 依赖的一种替代方案。
 *
 * <p>请注意，{@link org.springframework.core.io.Resource} 依赖也可以暴露为类型为 {@code Resource}
 * 或 {@code Resource[]} 的 Bean 属性，由 Bean 工厂通过自动类型转换从 String 填充。这样就不必仅仅为
 * 了访问特定文件资源而实现任何回调接口。
 *
 * <p>当应用程序对象必须访问名称经计算得出的各种文件资源时，通常需要 {@link ResourceLoader}。一种好
 * 的策略是让该对象使用 {@link org.springframework.core.io.DefaultResourceLoader}，但仍实现
 * {@code ResourceLoaderAware}，以便在 {@code ApplicationContext} 中运行时可以覆盖。有关示例，请参
 * 见 {@link org.springframework.context.support.ReloadableResourceBundleMessageSource}。
 *
 * <p>还可以检查传入的 {@code ResourceLoader} 是否实现了
 * {@link org.springframework.core.io.support.ResourcePatternResolver} 接口，并进行相应转换，以便将
 * 资源模式解析为 {@code Resource} 对象数组。在 ApplicationContext 中运行时这总是可行的（因为上下文
 * 接口扩展了 ResourcePatternResolver 接口）。可将
 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver} 用作默认实现；另
 * 请参见 {@code ResourcePatternUtils.getResourcePatternResolver} 方法。
 *
 * <p>作为 {@code ResourcePatternResolver} 依赖的替代方案，可以考虑暴露类型为 {@code Resource[]} 数
 * 组的 Bean 属性，在绑定时由 Bean 工厂通过自动类型转换从模式字符串填充。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 10.03.2004
 * @see ApplicationContextAware
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.core.io.support.ResourcePatternResolver
 */
public interface ResourceLoaderAware extends Aware {

	/**
	 * 设置此对象运行所在的 ResourceLoader。
	 *
	 * <p>这可能是一个 ResourcePatternResolver，可通过 {@code instanceof ResourcePatternResolver} 进行
	 * 检查。另请参见 {@code ResourcePatternUtils.getResourcePatternResolver} 方法。
	 *
	 * <p>在填充普通 Bean 属性之后、在诸如 InitializingBean 的 {@code afterPropertiesSet} 或自定义
	 * init-method 之类的初始化回调之前调用。在 ApplicationContextAware 的
	 * {@code setApplicationContext} 之前调用。
	 * @param resourceLoader the ResourceLoader object to be used by this object
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see org.springframework.core.io.support.ResourcePatternUtils#getResourcePatternResolver
	 */
	void setResourceLoader(ResourceLoader resourceLoader);

}
