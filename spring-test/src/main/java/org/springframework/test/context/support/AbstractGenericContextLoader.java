/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.StringUtils;

/**
 * 用于加载 {@link GenericApplicationContext} 的 {@link AbstractContextLoader} 的抽象通用扩展。
 *
 * <ul>
 * <li>如果通过 {@link org.springframework.test.context.ContextLoader ContextLoader} SPI 调用具体子类的实例，则将从传递给 {@link #loadContext(String...)} 的<em>位置</em>加载上下文。</li>
 * <li>如果通过 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} SPI 调用具体子类的实例，则将从传递给 {@link #loadContext(MergedContextConfiguration)} 的 {@link MergedContextConfiguration} 加载上下文。在这种情况下，{@code SmartContextLoader} 将决定是从<em>位置</em>还是从<em>注解类</em>加载上下文。</li>
 * </ul>
 *
 * <p>具体子类必须提供 {@link #createBeanDefinitionReader createBeanDefinitionReader()} 的适当实现，并且可能还需要重写 {@link #loadBeanDefinitions loadBeanDefinitions()}。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 2.5
 * @see #loadContext(MergedContextConfiguration)
 * @see #loadContext(String...)
 */
public abstract class AbstractGenericContextLoader extends AbstractContextLoader {

	protected static final Log logger = LogFactory.getLog(AbstractGenericContextLoader.class);


	/**
	 * 从所提供的 {@link MergedContextConfiguration} 加载 Spring ApplicationContext。
	 * <p>实现细节：
	 * <ul>
	 * <li>调用 {@link #validateMergedContextConfiguration(MergedContextConfiguration)} 以允许子类在继续之前验证所提供的配置。</li>
	 * <li>调用 {@link #createContext()} 创建 {@link GenericApplicationContext} 实例。</li>
	 * <li>如果所提供的 {@code MergedContextConfiguration} 引用了{@linkplain MergedContextConfiguration#getParent() 父配置}，则将获取相应的 {@link MergedContextConfiguration#getParentApplicationContext() ApplicationContext}，并将其{@linkplain GenericApplicationContext#setParent(ApplicationContext) 设置为父上下文}，作为此方法创建的上下文的父级。</li>
	 * <li>调用 {@link #prepareContext(GenericApplicationContext)} 以向后兼容 {@link org.springframework.test.context.ContextLoader ContextLoader} SPI。</li>
	 * <li>调用 {@link #prepareContext(ConfigurableApplicationContext, MergedContextConfiguration)} 以允许在加载 Bean 定义之前定制上下文。</li>
	 * <li>调用 {@link #customizeBeanFactory(DefaultListableBeanFactory)} 以允许定制上下文的 {@code DefaultListableBeanFactory}。</li>
	 * <li>委托给 {@link #loadBeanDefinitions(GenericApplicationContext, MergedContextConfiguration)}，以根据所提供的 {@code MergedContextConfiguration} 中的位置或类填充上下文。</li>
	 * <li>委托给 {@link AnnotationConfigUtils} 来{@link AnnotationConfigUtils#registerAnnotationConfigProcessors 注册}注解配置处理器。</li>
	 * <li>调用 {@link #customizeContext(GenericApplicationContext)} 以允许在刷新上下文之前定制上下文。</li>
	 * <li>调用 {@link #customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)} 以允许在刷新上下文之前定制上下文。</li>
	 * <li>{@link ConfigurableApplicationContext#refresh 刷新}上下文并为其注册 JVM 关闭钩子。</li>
	 * </ul>
	 * @return a new application context
	 * @since 3.1
	 * @see org.springframework.test.context.SmartContextLoader#loadContext(MergedContextConfiguration)
	 * @see GenericApplicationContext
	 */
	@Override
	public final ConfigurableApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Loading ApplicationContext for merged context configuration [%s].",
					mergedConfig));
		}

		validateMergedContextConfiguration(mergedConfig);

		GenericApplicationContext context = createContext();
		ApplicationContext parent = mergedConfig.getParentApplicationContext();
		if (parent != null) {
			context.setParent(parent);
		}

		prepareContext(context);
		prepareContext(context, mergedConfig);
		customizeBeanFactory(context.getDefaultListableBeanFactory());
		loadBeanDefinitions(context, mergedConfig);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		customizeContext(context);
		customizeContext(context, mergedConfig);

		context.refresh();
		context.registerShutdownHook();

		return context;
	}

	/**
	 * 针对此上下文加载器所支持的内容，验证所提供的 {@link MergedContextConfiguration}。
	 * <p>默认实现是<em>无操作</em>，但子类可以视需要重写它。
	 * @param mergedConfig the merged configuration to validate
	 * @throws IllegalStateException if the supplied configuration is not valid
	 * for this context loader
	 * @since 4.0.4
	 */
	protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		// no-op
	}

	/**
	 * 从所提供的 {@code locations} 加载 Spring ApplicationContext。
	 * <p>实现细节：
	 * <ul>
	 * <li>调用 {@link #createContext()} 创建 {@link GenericApplicationContext} 实例。</li>
	 * <li>调用 {@link #prepareContext(GenericApplicationContext)} 以允许在加载 Bean 定义之前定制上下文。</li>
	 * <li>调用 {@link #customizeBeanFactory(DefaultListableBeanFactory)} 以允许定制上下文的 {@code DefaultListableBeanFactory}。</li>
	 * <li>委托给 {@link #createBeanDefinitionReader(GenericApplicationContext)} 创建 {@link BeanDefinitionReader}，然后使用它从指定的位置填充上下文。</li>
	 * <li>委托给 {@link AnnotationConfigUtils} 来{@link AnnotationConfigUtils#registerAnnotationConfigProcessors 注册}注解配置处理器。</li>
	 * <li>调用 {@link #customizeContext(GenericApplicationContext)} 以允许在刷新上下文之前定制上下文。</li>
	 * <li>{@link ConfigurableApplicationContext#refresh 刷新}上下文并为其注册 JVM 关闭钩子。</li>
	 * </ul>
	 * <p><b>注意</b>：此方法不提供为所加载的上下文设置活动 Bean 定义配置文件的手段。有关替代方案，请参见 {@link #loadContext(MergedContextConfiguration)} 和 {@link AbstractContextLoader#prepareContext(ConfigurableApplicationContext, MergedContextConfiguration)}。
	 * @return a new application context
	 * @since 2.5
	 * @see org.springframework.test.context.ContextLoader#loadContext
	 * @see GenericApplicationContext
	 * @see #loadContext(MergedContextConfiguration)
	 */
	@Override
	public final ConfigurableApplicationContext loadContext(String... locations) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Loading ApplicationContext for locations [%s].",
					StringUtils.arrayToCommaDelimitedString(locations)));
		}

		GenericApplicationContext context = createContext();

		prepareContext(context);
		customizeBeanFactory(context.getDefaultListableBeanFactory());
		createBeanDefinitionReader(context).loadBeanDefinitions(locations);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		customizeContext(context);

		context.refresh();
		context.registerShutdownHook();

		return context;
	}

	/**
	 * 创建由此 {@code ContextLoader} 使用的 {@link GenericApplicationContext} 的工厂方法。
	 * <p>默认实现使用默认构造器创建 {@code GenericApplicationContext}。此方法可以被重写 &mdash; 例如，用于使用自定义上下文子类，或创建带有自定义 {@link DefaultListableBeanFactory} 实现的 {@code GenericApplicationContext}。
	 * @return a newly instantiated {@code GenericApplicationContext}
	 * @since 5.2.9
	 */
	protected GenericApplicationContext createContext() {
		return new GenericApplicationContext();
	}

	/**
	 * 准备由此 {@code ContextLoader} 创建的 {@link GenericApplicationContext}。在读取 Bean 定义<i>之前</i>调用。
	 * <p>默认实现为空。可在子类中重写，以定制 {@code GenericApplicationContext} 的标准设置。
	 * @param context the context that should be prepared
	 * @since 2.5
	 * @see #loadContext(MergedContextConfiguration)
	 * @see #loadContext(String...)
	 * @see GenericApplicationContext#setAllowBeanDefinitionOverriding
	 * @see GenericApplicationContext#setResourceLoader
	 * @see GenericApplicationContext#setId
	 * @see #prepareContext(ConfigurableApplicationContext, MergedContextConfiguration)
	 */
	protected void prepareContext(GenericApplicationContext context) {
	}

	/**
	 * 定制由此 {@code ContextLoader} 创建的 ApplicationContext 的内部 Bean 工厂。
	 * <p>默认实现为空，但可在子类中重写，以定制 {@code DefaultListableBeanFactory} 的标准设置。
	 * @param beanFactory the bean factory created by this {@code ContextLoader}
	 * @since 2.5
	 * @see #loadContext(MergedContextConfiguration)
	 * @see #loadContext(String...)
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see DefaultListableBeanFactory#setAllowCircularReferences
	 * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
	}

	/**
	 * 从所提供的 {@code MergedContextConfiguration} 中的位置或类，将 Bean 定义加载到所提供的 {@link GenericApplicationContext context} 中。
	 * <p>默认实现委托给由 {@link #createBeanDefinitionReader(GenericApplicationContext)} 返回的 {@link BeanDefinitionReader}，以{@link BeanDefinitionReader#loadBeanDefinitions(String) 加载} Bean 定义。
	 * <p>子类必须提供 {@link #createBeanDefinitionReader(GenericApplicationContext)} 的适当实现。或者，子类可以提供 {@code createBeanDefinitionReader()} 的<em>无操作</em>实现，并重写此方法以提供加载或注册 Bean 定义的自定义策略。
	 * @param context the context into which the bean definitions should be loaded
	 * @param mergedConfig the merged context configuration
	 * @since 3.1
	 * @see #loadContext(MergedContextConfiguration)
	 */
	protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
		createBeanDefinitionReader(context).loadBeanDefinitions(mergedConfig.getLocations());
	}

	/**
	 * 为将 Bean 定义加载到所提供的 {@link GenericApplicationContext context} 中而创建新的 {@link BeanDefinitionReader} 的工厂方法。
	 * @param context the context for which the {@code BeanDefinitionReader}
	 * should be created
	 * @return a {@code BeanDefinitionReader} for the supplied context
	 * @since 2.5
	 * @see #loadContext(String...)
	 * @see #loadBeanDefinitions
	 * @see BeanDefinitionReader
	 */
	protected abstract BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context);

	/**
	 * 在 Bean 定义已加载到上下文<i>之后</i>但上下文刷新<i>之前</i>，定制由该 {@code ContextLoader} 创建的 {@link GenericApplicationContext}。
	 * <p>默认实现为空，但可在子类中重写，以定制应用上下文。
	 * @param context the newly created application context
	 * @since 2.5
	 * @see #loadContext(MergedContextConfiguration)
	 * @see #loadContext(String...)
	 * @see #customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)
	 */
	protected void customizeContext(GenericApplicationContext context) {
	}

}
