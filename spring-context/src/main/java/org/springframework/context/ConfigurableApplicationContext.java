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

package org.springframework.context;

import java.io.Closeable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.lang.Nullable;

/**
 * 供大多数（即使不是全部）应用上下文实现的 SPI 接口。
 * 除了 {@link org.springframework.context.ApplicationContext} 接口中的应用上下文客户端方法之外，还
 * 提供了配置应用上下文的设施。
 *
 * <p>这里封装了配置和生命周期方法，以避免它们对 ApplicationContext 客户端代码一览无余。这些方法只
 * 应由启动和关闭代码使用。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 03.11.2003
 */
public interface ConfigurableApplicationContext extends ApplicationContext, Lifecycle, Closeable {

	/**
	 * 这些字符中的任意数量都被视为单个 String 值中多个上下文配置路径之间的分隔符。
	 * @see org.springframework.context.support.AbstractXmlApplicationContext#setConfigLocation
	 * @see org.springframework.web.context.ContextLoader#CONFIG_LOCATION_PARAM
	 * @see org.springframework.web.servlet.FrameworkServlet#setContextConfigLocation
	 */
	String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

	/**
	 * 工厂中 ConversionService Bean 的名称。
	 * 如果未提供，则应用默认转换规则。
	 * @since 3.0
	 * @see org.springframework.core.convert.ConversionService
	 */
	String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

	/**
	 * 工厂中 LoadTimeWeaver Bean 的名称。如果提供了这样的 Bean，上下文将使用临时 ClassLoader 进行类型
	 * 匹配，以便让 LoadTimeWeaver 处理所有实际的 Bean 类。
	 * @since 2.5
	 * @see org.springframework.instrument.classloading.LoadTimeWeaver
	 */
	String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";

	/**
	 * 工厂中 {@link Environment} Bean 的名称。
	 * @since 3.1
	 */
	String ENVIRONMENT_BEAN_NAME = "environment";

	/**
	 * 工厂中系统属性 Bean 的名称。
	 * @see java.lang.System#getProperties()
	 */
	String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";

	/**
	 * 工厂中系统环境 Bean 的名称。
	 * @see java.lang.System#getenv()
	 */
	String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";

	/**
	 * 工厂中 {@link ApplicationStartup} Bean 的名称。
	 * @since 5.3
	 */
	String APPLICATION_STARTUP_BEAN_NAME = "applicationStartup";

	/**
	 * {@linkplain #registerShutdownHook() 关闭钩子} 线程的 {@link Thread#getName() 名称}：{@value}。
	 * @since 5.2
	 * @see #registerShutdownHook()
	 */
	String SHUTDOWN_HOOK_THREAD_NAME = "SpringContextShutdownHook";


	/**
	 * 设置此应用上下文的唯一 id。
	 * @since 3.0
	 */
	void setId(String id);

	/**
	 * 设置此应用上下文的父上下文。
	 *
	 * <p>请注意，父上下文不应被更改：只有在创建此类的对象时父上下文不可用（例如在
	 * WebApplicationContext 设置的情况下）时，才应在构造函数之外设置它。
	 * @param parent the parent context
	 * @see org.springframework.web.context.ConfigurableWebApplicationContext
	 */
	void setParent(@Nullable ApplicationContext parent);

	/**
	 * 为此应用上下文设置 {@code Environment}。
	 * @param environment the new environment
	 * @since 3.1
	 */
	void setEnvironment(ConfigurableEnvironment environment);

	/**
	 * 以可配置形式返回此应用上下文的 {@code Environment}，允许进一步自定义。
	 * @since 3.1
	 */
	@Override
	ConfigurableEnvironment getEnvironment();

	/**
	 * 为此应用上下文设置 {@link ApplicationStartup}。
	 *
	 * <p>这允许应用上下文在启动期间记录指标。
	 * @param applicationStartup the new context event factory
	 * @since 5.3
	 */
	void setApplicationStartup(ApplicationStartup applicationStartup);

	/**
	 * 返回此应用上下文的 {@link ApplicationStartup}。
	 * @since 5.3
	 */
	ApplicationStartup getApplicationStartup();

	/**
	 * 添加一个新的 BeanFactoryPostProcessor，它将在刷新时、在任何 Bean 定义被求值之前应用于此应用上下
	 * 文的内部 Bean 工厂。应在上下文配置期间调用。
	 * @param postProcessor the factory processor to register
	 */
	void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);

	/**
	 * 添加一个新的 ApplicationListener，它将在上下文事件（如上下文刷新和上下文关闭）发生时收到通知。
	 *
	 * <p>请注意，在此注册的任何 ApplicationListener，如果上下文尚未激活，则会在刷新时应用；如果上下文
	 * 已经激活，则会通过当前的事件广播器即时应用。
	 * @param listener the ApplicationListener to register
	 * @see org.springframework.context.event.ContextRefreshedEvent
	 * @see org.springframework.context.event.ContextClosedEvent
	 */
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * 指定用于加载类路径资源和 Bean 类的 ClassLoader。
	 *
	 * <p>此上下文类加载器将被传递给内部 Bean 工厂。
	 * @since 5.2.7
	 * @see org.springframework.core.io.DefaultResourceLoader#DefaultResourceLoader(ClassLoader)
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setBeanClassLoader
	 */
	void setClassLoader(ClassLoader classLoader);

	/**
	 * 向此应用上下文注册给定的协议解析器，从而可以处理额外的资源协议。
	 *
	 * <p>任何此类解析器都将在此上下文的标准解析规则之前被调用。因此，它也可能覆盖任何默认规则。
	 * @since 4.3
	 */
	void addProtocolResolver(ProtocolResolver resolver);

	/**
	 * 加载或刷新配置的持久化表示形式，它可能来自基于 Java 的配置、XML 文件、属性文件、关系数据库模式或
	 * 某种其他格式。
	 *
	 * <p>由于这是一个启动方法，因此如果失败，它应销毁已经创建的单例，以避免资源悬空。换言之，在调用此
	 * 方法之后，要么所有单例都已实例化，要么一个单例都没有实例化。
	 * @throws BeansException if the bean factory could not be initialized
	 * @throws IllegalStateException if already initialized and multiple refresh
	 * attempts are not supported
	 */
	void refresh() throws BeansException, IllegalStateException;

	/**
	 * 向 JVM 运行时注册一个关闭钩子，在 JVM 关闭时关闭此上下文，除非届时它已经关闭。
	 *
	 * <p>此方法可以多次调用。每个上下文实例最多只会注册一个关闭钩子。
	 *
	 * <p>从 Spring Framework 5.2 起，关闭钩子线程的 {@linkplain Thread#getName() 名称} 应为
	 * {@link #SHUTDOWN_HOOK_THREAD_NAME}。
	 * @see java.lang.Runtime#addShutdownHook
	 * @see #close()
	 */
	void registerShutdownHook();

	/**
	 * 关闭此应用上下文，释放该实现可能持有的所有资源和锁。这包括销毁所有缓存的单例 Bean。
	 *
	 * <p>注意：<i>不会</i> 在父上下文上调用 {@code close}；父上下文有自己的独立生命周期。
	 *
	 * <p>此方法可以多次调用而不会产生副作用：对已关闭上下文的后续 {@code close} 调用将被忽略。
	 */
	@Override
	void close();

	/**
	 * 确定此应用上下文是否处于活动状态，即它是否已至少刷新过一次且尚未关闭。
	 * @return whether the context is still active
	 * @see #refresh()
	 * @see #close()
	 * @see #getBeanFactory()
	 */
	boolean isActive();

	/**
	 * 返回此应用上下文的内部 Bean 工厂。
	 * 可用于访问底层工厂的特定功能。
	 *
	 * <p>注意：不要用它来对 Bean 工厂进行后处理；在此之前单例就已经实例化了。请使用
	 * BeanFactoryPostProcessor 在 Bean 被触碰之前拦截 BeanFactory 的设置过程。
	 *
	 * <p>通常，只有上下文处于活动状态时（即在 {@link #refresh()} 和 {@link #close()} 之间），才能访问
	 * 此内部工厂。可以使用 {@link #isActive()} 标志检查上下文是否处于适当状态。
	 * @return the underlying bean factory
	 * @throws IllegalStateException if the context does not hold an internal
	 * bean factory (usually if {@link #refresh()} hasn't been called yet or
	 * if {@link #close()} has already been called)
	 * @see #isActive()
	 * @see #refresh()
	 * @see #close()
	 * @see #addBeanFactoryPostProcessor
	 */
	ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

}
