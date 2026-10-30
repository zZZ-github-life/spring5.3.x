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

/**
 * 表示当前应用程序运行所处环境的接口。
 * 对应用程序环境的两个关键方面进行建模：<em>profile</em> 和 <em>属性</em>。
 * 与属性访问相关的方法通过 {@link PropertyResolver} 父接口暴露。
 *
 * <p><em>profile</em> 是一组具名的 Bean 定义逻辑分组，仅当给定 profile 处于
 * <em>活动</em>状态时才会注册到容器中。Bean 无论是以 XML 还是通过注解定义，
 * 都可以被分配到 profile；有关语法细节，请参见 spring-beans 3.1 schema
 * 或 {@link org.springframework.context.annotation.Profile @Profile} 注解。
 * {@code Environment} 对象在 profile 方面的作用在于确定当前哪些 profile（如果有）
 * 处于{@linkplain #getActiveProfiles 活动}状态，以及哪些 profile（如果有）
 * 应默认{@linkplain #getDefaultProfiles 处于活动}状态。
 *
 * <p><em>属性</em>在几乎所有应用程序中都扮演着重要角色，其来源多种多样：
 * 属性文件、JVM 系统属性、系统环境变量、JNDI、Servlet 上下文参数、
 * 临时的 Properties 对象、Map 等等。{@code Environment} 对象在属性方面的作用
 * 是为用户提供一个便捷的服务接口，用于配置属性源并从中解析属性。
 *
 * <p>在 {@code ApplicationContext} 中管理的 Bean 可以注册为
 * {@link org.springframework.context.EnvironmentAware EnvironmentAware}，
 * 或者通过 {@code @Inject} 注入 {@code Environment}，以便查询 profile 状态或直接解析属性。
 *
 * <p>不过在大多数情况下，应用级别的 Bean 不应需要直接与 {@code Environment} 交互，
 * 而是可以让 {@code ${...}} 属性值由属性占位符配置器替换，例如
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer PropertySourcesPlaceholderConfigurer}，
 *
 * 它本身实现了 {@code EnvironmentAware}，并且从 Spring 3.1 起在使用
 * {@code <context:property-placeholder/>} 时默认注册。
 *
 * <p>{@code Environment} 对象的配置必须通过 {@code ConfigurableEnvironment} 接口完成，
 * 该接口由所有 {@code AbstractApplicationContext} 子类的 {@code getEnvironment()} 方法返回。
 * 有关在应用上下文 {@code refresh()} 之前操作属性源的用法示例，
 * 请参见 {@link ConfigurableEnvironment} 的 Javadoc。
 *
 * @author Chris Beams
 * @since 3.1
 * @see PropertyResolver
 * @see EnvironmentCapable
 * @see ConfigurableEnvironment
 * @see AbstractEnvironment
 * @see StandardEnvironment
 * @see org.springframework.context.EnvironmentAware
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#setEnvironment
 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
 */
public interface Environment extends PropertyResolver {

	/**
	 * 返回为此环境显式激活的 profile 集合。profile 用于创建按条件注册的
	 * Bean 定义的逻辑分组，例如基于部署环境。可以通过将
	 * {@linkplain AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME "spring.profiles.active"}
	 * 设置为系统属性，或调用 {@link ConfigurableEnvironment#setActiveProfiles(String...)}
	 * 来激活 profile。
	 * <p>如果未显式指定任何活动 profile，则会自动激活所有
	 * {@linkplain #getDefaultProfiles() 默认 profile}。
	 * @see #getDefaultProfiles
	 * @see ConfigurableEnvironment#setActiveProfiles
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	String[] getActiveProfiles();

	/**
	 * 返回在未显式设置任何活动 profile 时默认应处于活动状态的 profile 集合。
	 * @see #getActiveProfiles
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 */
	String[] getDefaultProfiles();

	/**
	 * 返回给定 profile 中的一个或多个是否处于活动状态；或者在没有显式活动 profile 的情况下，
	 * 返回给定 profile 中的一个或多个是否包含在默认 profile 集合中。如果 profile 以 '!' 开头，
	 * 则逻辑反转，即如果给定 profile <em>不</em>处于活动状态，该方法将返回 {@code true}。
	 * 例如，如果 profile 'p1' 处于活动状态或 'p2' 不处于活动状态，
	 * {@code env.acceptsProfiles("p1", "!p2")} 将返回 {@code true}。
	 * @throws IllegalArgumentException 如果以零个参数调用，或任一 profile 为 {@code null}、
	 * 为空或仅包含空白字符
	 * @see #getActiveProfiles
	 * @see #getDefaultProfiles
	 * @see #acceptsProfiles(Profiles)
	 * @deprecated as of 5.1 in favor of {@link #acceptsProfiles(Profiles)}
	 */
	@Deprecated
	boolean acceptsProfiles(String... profiles);

	/**
	 * 返回{@linkplain #getActiveProfiles() 活动 profile}是否与给定的 {@link Profiles} 谓词匹配。
	 */
	boolean acceptsProfiles(Profiles profiles);

}
