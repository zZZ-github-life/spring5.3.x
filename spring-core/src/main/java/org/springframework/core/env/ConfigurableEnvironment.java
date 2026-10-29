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

package org.springframework.core.env;

import java.util.Map;

/**
 * 配置接口，由大多数（如果不是全部）{@link Environment} 类型实现。提供设置活动 profile 和默认
 * profile 以及操作底层属性源的功能。允许客户端通过 {@link ConfigurablePropertyResolver} 父接口设置
 * 和校验必需属性、自定义转换服务等。
 *
 * <h2>操作属性源</h2>
 * <p>属性源可以被移除、重新排序或替换；也可以使用从 {@link #getPropertySources()} 返回的
 * {@link MutablePropertySources} 实例添加额外的属性源。以下示例针对
 * {@code ConfigurableEnvironment} 的 {@link StandardEnvironment} 实现，但通常适用于任何实现，尽管
 * 具体的默认属性源可能不同。
 *
 * <h4>示例：添加具有最高搜索优先级的新属性源</h4>
 * <pre class="code">
 * ConfigurableEnvironment environment = new StandardEnvironment();
 * MutablePropertySources propertySources = environment.getPropertySources();
 * Map&lt;String, String&gt; myMap = new HashMap&lt;&gt;();
 * myMap.put("xyz", "myValue");
 * propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
 * </pre>
 *
 * <h4>示例：移除默认的系统属性属性源</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
 * </pre>
 *
 * <h4>示例：为测试目的模拟系统环境</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
 * propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
 * </pre>
 *
 * 当 {@link Environment} 被 {@code ApplicationContext} 使用时，重要的是在调用上下文的
 * {@link org.springframework.context.support.AbstractApplicationContext#refresh() refresh()} 方法
 * <em>之前</em>执行此类 {@code PropertySource} 操作。这确保所有属性源在容器引导过程中都可用，包括
 * {@linkplain org.springframework.context.support.PropertySourcesPlaceholderConfigurer 属性占位符配置器}
 * 的使用。
 *
 * @author Chris Beams
 * @since 3.1
 * @see StandardEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 */
public interface ConfigurableEnvironment extends Environment, ConfigurablePropertyResolver {

	/**
	 * 指定此 {@code Environment} 的活动 profile 集合。profile 会在容器引导期间被评估，以确定是否应将
	 * Bean 定义注册到容器中。
	 * <p>任何现有的活动 profile 都将被给定参数替换；使用零个参数调用可清除当前活动 profile 集合。使用
	 * {@link #addActiveProfile} 可在保留现有集合的同时添加 profile。
	 * @throws IllegalArgumentException if any profile is null, empty or whitespace-only
	 * @see #addActiveProfile
	 * @see #setDefaultProfiles
	 * @see org.springframework.context.annotation.Profile
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	void setActiveProfiles(String... profiles);

	/**
	 * 向当前活动 profile 集合中添加一个 profile。
	 * @throws IllegalArgumentException if the profile is null, empty or whitespace-only
	 * @see #setActiveProfiles
	 */
	void addActiveProfile(String profile);

	/**
	 * 指定在没有通过 {@link #setActiveProfiles} 显式激活其他 profile 时默认激活的 profile 集合。
	 * @throws IllegalArgumentException if any profile is null, empty or whitespace-only
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 */
	void setDefaultProfiles(String... profiles);

	/**
	 * 以可变形式返回此 {@code Environment} 的 {@link PropertySources}，允许操作在针对此
	 * {@code Environment} 对象解析属性时应搜索的 {@link PropertySource} 对象集合。诸如
	 * {@link MutablePropertySources#addFirst addFirst}、{@link MutablePropertySources#addLast addLast}、
	 * {@link MutablePropertySources#addBefore addBefore} 和
	 * {@link MutablePropertySources#addAfter addAfter} 等各种 {@link MutablePropertySources} 方法可对
	 * 属性源顺序进行细粒度控制。例如，这在确保某些用户定义的属性源比系统属性集合或系统环境变量集合等默
	 * 认属性源具有更高搜索优先级时很有用。
	 * @see AbstractEnvironment#customizePropertySources
	 */
	MutablePropertySources getPropertySources();

	/**
	 * 如果当前 {@link SecurityManager} 允许，则返回 {@link System#getProperties()} 的值，否则返回一个
	 * Map 实现，该实现将尝试通过调用 {@link System#getProperty(String)} 访问各个键。
	 * <p>请注意，大多数 {@code Environment} 实现都会将此系统属性 Map 作为默认的{@link PropertySource}
	 * 纳入搜索。因此，除非明确打算绕过其他属性源，否则建议不要直接使用此方法。
	 * <p>对所返回 Map 调用 {@link Map#get(Object)} 将永远不会抛出 {@link IllegalAccessException}；在
	 * SecurityManager 禁止访问某个属性的情况下，将返回 {@code null}，并发出 INFO 级别的日志消息说明该
	 * 异常。
	 */
	Map<String, Object> getSystemProperties();

	/**
	 * 如果当前 {@link SecurityManager} 允许，则返回 {@link System#getenv()} 的值，否则返回一个 Map 实
	 * 现，该实现将尝试通过调用 {@link System#getenv(String)} 访问各个键。
	 * <p>请注意，大多数 {@link Environment} 实现都会将此系统环境 Map 作为默认的{@link PropertySource}
	 * 纳入搜索。因此，除非明确打算绕过其他属性源，否则建议不要直接使用此方法。
	 * <p>对所返回 Map 调用 {@link Map#get(Object)} 将永远不会抛出 {@link IllegalAccessException}；在
	 * SecurityManager 禁止访问某个属性的情况下，将返回 {@code null}，并发出 INFO 级别的日志消息说明该
	 * 异常。
	 */
	Map<String, Object> getSystemEnvironment();

	/**
	 * 将给定父环境的活动 profile、默认 profile 和属性源分别追加到此（子）环境对应的集合中。
	 * <p>对于父子双方都存在的同名 {@code PropertySource} 实例，将保留子实例并丢弃父实例。这样做既可以
	 * 允许子环境覆盖属性源，又能避免对常见属性源类型（例如系统环境和系统属性）进行冗余搜索。
	 * <p>活动 profile 和默认 profile 名称也会进行去重过滤，以避免混淆和冗余存储。
	 * <p>在任何情况下，父环境都保持不变。请注意，在调用 {@code merge} 之后对父环境所做的任何更改都不会
	 * 反映到子环境中。因此，在调用 {@code merge} 之前应谨慎配置父属性源和 profile 信息。
	 * @param parent the environment to merge with
	 * @since 3.1.2
	 * @see org.springframework.context.support.AbstractApplicationContext#setParent
	 */
	void merge(ConfigurableEnvironment parent);

}
