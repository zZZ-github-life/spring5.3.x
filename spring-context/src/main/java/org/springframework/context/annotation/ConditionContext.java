/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * 供 {@link Condition} 实现使用的上下文信息。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface ConditionContext {

	/**
	 * 如果条件匹配，返回将持有 Bean 定义的 {@link BeanDefinitionRegistry}。
	 * @throws IllegalStateException if no registry is available (which is unusual:
	 * only the case with a plain {@link ClassPathScanningCandidateComponentProvider})
	 */
	BeanDefinitionRegistry getRegistry();

	/**
	 * 如果条件匹配，返回将持有 Bean 定义的 {@link ConfigurableListableBeanFactory}；如果 Bean 工厂不可
	 * 用（或无法向下转换为 {@code ConfigurableListableBeanFactory}），则返回 {@code null}。
	 */
	@Nullable
	ConfigurableListableBeanFactory getBeanFactory();

	/**
	 * 返回当前应用运行所在的 {@link Environment}。
	 */
	Environment getEnvironment();

	/**
	 * 返回当前正在使用的 {@link ResourceLoader}。
	 */
	ResourceLoader getResourceLoader();

	/**
	 * 返回应用于加载其他类的 {@link ClassLoader}（仅当连系统 ClassLoader 都不可访问时才为 {@code null}）。
	 *
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getClassLoader();

}
