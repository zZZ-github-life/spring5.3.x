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

package org.springframework.core.env;

/**
 * 适用于“标准”（即非 Web）应用程序的 {@link Environment} 实现。
 *
 * <p>除了 {@link ConfigurableEnvironment} 的常规功能（如属性解析和 profile 相关操作）之外，
 * 此实现还配置了两个默认属性源，按以下顺序搜索：
 * <ul>
 * <li>{@linkplain AbstractEnvironment#getSystemProperties() system properties}
 * <li>{@linkplain AbstractEnvironment#getSystemEnvironment() system environment variables}
 * </ul>
 *
 * 也就是说，如果键 "xyz" 同时存在于 JVM 系统属性和当前进程的环境变量集中，
 * 则调用 {@code environment.getProperty("xyz")} 将返回系统属性中键 "xyz" 的值。
 * 默认选择这种顺序，是因为系统属性是按 JVM 独立的，
 * 而环境变量在给定系统上的多个 JVM 之间可能相同。让系统属性具有更高优先级，
 * 可以在每个 JVM 基础上覆盖环境变量。
 *
 * <p>这些默认属性源可以被移除、重新排序或替换；
 * 并且可以使用 {@link #getPropertySources()} 提供的 {@link MutablePropertySources} 实例
 * 添加其他属性源。用法示例请参见 {@link ConfigurableEnvironment} 的 Javadoc。
 *
 * <p>有关 shell 环境（例如 Bash）中属性名特殊处理的详细信息
 * （这些环境不允许变量名中包含句点字符），请参见
 * {@link SystemEnvironmentPropertySource} 的 javadoc。
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.1
 * @see ConfigurableEnvironment
 * @see SystemEnvironmentPropertySource
 * @see org.springframework.web.context.support.StandardServletEnvironment
 */
public class StandardEnvironment extends AbstractEnvironment {

	/** 系统环境属性源名称：{@value}。 */
	public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";

	/** JVM 系统属性属性源名称：{@value}。 */
	public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";


	/**
	 * 使用默认的 {@link MutablePropertySources} 实例创建一个新的 {@code StandardEnvironment} 实例。
	 */
	public StandardEnvironment() {
	}

	/**
	 * 使用特定的 {@link MutablePropertySources} 实例创建一个新的 {@code StandardEnvironment} 实例。
	 * @param propertySources property sources to use
	 * @since 5.3.4
	 */
	protected StandardEnvironment(MutablePropertySources propertySources) {
		super(propertySources);
	}


	/**
	 * 使用适合任何标准 Java 环境的属性源来定制属性源集合：
	 * <ul>
	 * <li>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}
	 * <li>{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}
	 * </ul>
	 * <p>存在于 {@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME} 中的属性
	 * 将优先于 {@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME} 中的属性。
	 * @see AbstractEnvironment#customizePropertySources(MutablePropertySources)
	 * @see #getSystemProperties()
	 * @see #getSystemEnvironment()
	 */
	@Override
	protected void customizePropertySources(MutablePropertySources propertySources) {
		propertySources.addLast(
				new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));
		propertySources.addLast(
				new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
	}

}
