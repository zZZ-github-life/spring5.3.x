/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context;

import org.springframework.context.ApplicationContext;

/**
 * 用于为 Spring TestContext Framework 管理的集成测试加载
 * {@link ApplicationContext 应用程序上下文}的策略接口。
 *
 * <p>{@code SmartContextLoader} SPI 取代了 Spring 2.5 引入的 {@link ContextLoader} SPI：
 * {@code SmartContextLoader} 可以选择处理资源位置或注解类。此外，
 * {@code SmartContextLoader} 可以在其加载的
 * 上下文中设置活动 Bean 定义配置（见 {@link MergedContextConfiguration#getActiveProfiles()}
 * 和 {@link #loadContext(MergedContextConfiguration)}）。
 *
 * <p>有关<em>注解类</em>的定义，请参见 {@link ContextConfiguration @ContextConfiguration}
 * 的 Javadoc。
 *
 * <p>{@code SmartContextLoader} 的客户端应在
 * 调用
 * {@link #loadContext(MergedContextConfiguration) loadContext()} 之前调用
 * {@link #processContextConfiguration(ContextConfigurationAttributes)
 * processContextConfiguration()}。这使 {@code SmartContextLoader}
 * 有机会为修改资源位置或检测默认资源位置或
 * 默认配置类提供自定义支持。
 * {@link #processContextConfiguration(ContextConfigurationAttributes)
 * processContextConfiguration()} 的结果应针对根测试类层次结构中的所有类进行合并，
 * 然后提供给
 * {@link #loadContext(MergedContextConfiguration) loadContext()}。
 *
 * <p>尽管 {@code SmartContextLoader} 扩展了 {@code ContextLoader}，
 * 客户端应优先使用 {@code SmartContextLoader} 特有的方法，
 * 而不是 {@code ContextLoader} 中定义的方法，特别是因为
 * {@code SmartContextLoader} 可能选择不支持
 * {@code ContextLoader} SPI 中定义的方法。
 *
 * <p>具体实现必须提供 {@code public} 无参构造函数。
 *
 * <p>Spring 提供以下开箱即用的实现：
 * <ul>
 * <li>{@link org.springframework.test.context.support.DelegatingSmartContextLoader DelegatingSmartContextLoader}</li>
 * <li>{@link org.springframework.test.context.support.AnnotationConfigContextLoader AnnotationConfigContextLoader}</li>
 * <li>{@link org.springframework.test.context.support.GenericXmlContextLoader GenericXmlContextLoader}</li>
 * <li>{@link org.springframework.test.context.support.GenericPropertiesContextLoader GenericPropertiesContextLoader}</li>
 * <li>{@link org.springframework.test.context.web.WebDelegatingSmartContextLoader WebDelegatingSmartContextLoader}</li>
 * <li>{@link org.springframework.test.context.web.AnnotationConfigWebContextLoader AnnotationConfigWebContextLoader}</li>
 * <li>{@link org.springframework.test.context.web.GenericXmlWebContextLoader GenericXmlWebContextLoader}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 3.1
 * @see ContextConfiguration
 * @see ActiveProfiles
 * @see ContextConfigurationAttributes
 * @see MergedContextConfiguration
 */
public interface SmartContextLoader extends ContextLoader {

	/**
	 * 处理给定测试类的 {@link ContextConfigurationAttributes}。
	 * <p>具体实现可以选择<em>修改</em>提供的 {@link ContextConfigurationAttributes}
	 * 中的 {@code locations} 或 {@code classes}，
	 * <em>生成</em>默认配置位置，或在提供的值为 {@code null}
	 * 或空时<em>检测</em>默认配置类。
	 * <p><b>注意</b>：与标准 {@code ContextLoader} 不同，
	 * {@code SmartContextLoader} <b>必须</b><em>预先</em>验证
	 * 生成或检测到的默认值确实存在，然后再在提供的
	 * {@link ContextConfigurationAttributes} 中设置相应的
	 * {@code locations} 或 {@code classes} 属性。因此，将
	 * {@code locations} 或 {@code classes} 属性保留为空表示
	 * 此 {@code SmartContextLoader} 无法生成或检测到默认值。
	 * @param configAttributes the context configuration attributes to process
	 */
	void processContextConfiguration(ContextConfigurationAttributes configAttributes);

	/**
	 * 根据提供的
	 * {@link MergedContextConfiguration 合并上下文配置}加载新的 {@link ApplicationContext 上下文}，
	 * 配置该上下文，最后以完全
	 * <em>刷新</em>的状态返回该上下文。
	 * <p>具体实现应为此
	 * {@code SmartContextLoader} 加载的
	 * {@link ApplicationContext 应用程序上下文}的 Bean 工厂注册注解配置
	 * 处理器。因此，Bean 将自动
	 * 成为使用
	 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired}、
	 * {@link javax.annotation.Resource @Resource} 和
	 * {@link javax.inject.Inject @Inject} 进行基于注解的依赖注入的候选者。此外，具体实现
	 * 应在上下文的
	 * {@link org.springframework.core.env.Environment Environment} 中设置活动 Bean 定义配置。
	 * <p>{@code SmartContextLoader} 加载的任何 {@code ApplicationContext}
	 * <strong>必须</strong>为其自身注册 JVM
	 * 关闭钩子。除非上下文提前关闭，否则所有上下文
	 * 实例将在 JVM 关闭时自动关闭。这样可以
	 * 释放上下文中 Bean 持有的外部资源（例如
	 * 临时文件）。
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @return a new application context
	 * @throws Exception if context loading failed
	 * @see #processContextConfiguration(ContextConfigurationAttributes)
	 * @see org.springframework.context.annotation.AnnotationConfigUtils
	 * #registerAnnotationConfigProcessors(org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 * @see MergedContextConfiguration#getActiveProfiles()
	 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment()
	 */
	ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception;

}
