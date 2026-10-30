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

package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.Assert;

/**
 * {@code AbstractDelegatingSmartContextLoader} 作为 {@link SmartContextLoader} SPI 实现的抽象基类，这些实现委托给一组<em>候选</em> SmartContextLoader（即一个支持 XML 配置文件或 Groovy 脚本，另一个支持注解类），以确定为给定测试类的配置使用哪个上下文加载器。每个候选加载器都有机会为测试类层次结构及外围类层次结构中每个带有 {@link ContextConfiguration @ContextConfiguration} 注解的类{@linkplain #processContextConfiguration 处理} {@link ContextConfigurationAttributes}，然后支持合并处理后的配置的候选加载器将被用于实际{@linkplain #loadContext 加载}上下文。
 *
 * <p>任何对<em>基于 XML 的加载器</em>的引用都可理解为仅支持 XML 配置文件的上下文加载器，或同时支持 XML 配置文件和 Groovy 脚本的上下文加载器。
 *
 * <p>在测试类上放置一个空的 {@code @ContextConfiguration} 注解表示应检测默认资源位置（例如 XML 配置文件或 Groovy 脚本）或默认的 {@linkplain org.springframework.context.annotation.Configuration 配置类}。此外，如果未通过 {@code @ContextConfiguration} 显式声明特定的 {@link ContextLoader} 或 {@link SmartContextLoader}，则将使用 {@code AbstractDelegatingSmartContextLoader} 的具体子类作为默认加载器，从而自动支持基于路径的资源位置（例如 XML 配置文件和 Groovy 脚本）或注解类，但不能同时支持两者。
 *
 * <p>从 Spring 3.2 开始，测试类可以选择既不声明基于路径的资源位置，也不声明注解类，而仅声明 {@linkplain ContextConfiguration#initializers 应用上下文初始化器}。在这种情况下，仍将尝试检测默认值，但如果不存在默认值，也不会导致异常。
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.2
 * @see SmartContextLoader
 */
public abstract class AbstractDelegatingSmartContextLoader implements SmartContextLoader {

	private static final Log logger = LogFactory.getLog(AbstractDelegatingSmartContextLoader.class);


	/**
	 * 获取支持 XML 配置文件和/或 Groovy 脚本的委托 {@code SmartContextLoader}。
	 */
	protected abstract SmartContextLoader getXmlLoader();

	/**
	 * 获取支持注解类的委托 {@code SmartContextLoader}。
	 */
	protected abstract SmartContextLoader getAnnotationConfigLoader();


	// ContextLoader

	/**
	 * {@code AbstractDelegatingSmartContextLoader} 不支持 {@link ContextLoader#processLocations(Class, String...)} 方法。请改用 {@link #processContextConfiguration(ContextConfigurationAttributes)}。
	 * @throws UnsupportedOperationException 在此实现中
	 */
	@Override
	public final String[] processLocations(Class<?> clazz, @Nullable String... locations) {
		throw new UnsupportedOperationException(
				"DelegatingSmartContextLoaders do not support the ContextLoader SPI. " +
						"Call processContextConfiguration(ContextConfigurationAttributes) instead.");
	}

	/**
	 * {@code AbstractDelegatingSmartContextLoader} 不支持 {@link ContextLoader#loadContext(String...) } 方法。请改用 {@link #loadContext(MergedContextConfiguration)}。
	 * @throws UnsupportedOperationException 在此实现中
	 */
	@Override
	public final ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException(
				"DelegatingSmartContextLoaders do not support the ContextLoader SPI. " +
						"Call loadContext(MergedContextConfiguration) instead.");
	}


	// SmartContextLoader

	/**
	 * 委托给候选 {@code SmartContextLoaders} 来处理所提供的 {@link ContextConfigurationAttributes}。
	 * <p>委托基于对 {@linkplain #getXmlLoader() XML 配置文件和 Groovy 脚本}以及 {@linkplain #getAnnotationConfigLoader() 注解类}的默认加载器实现的明确了解。具体而言，委托算法如下：
	 * <ul>
	 * <li>如果所提供的 {@code ContextConfigurationAttributes} 中的资源位置或注解类不为空，则将允许相应的候选加载器<em>按原样</em>处理该配置，而不进行任何默认值检测检查。</li>
	 * <li>否则，将允许基于 XML 的加载器处理该配置以检测默认资源位置。如果基于 XML 的加载器检测到默认资源位置，将记录一条 {@code info} 消息。</li>
	 * <li>随后，将允许基于注解的加载器处理该配置以检测默认配置类。如果基于注解的加载器检测到默认配置类，将记录一条 {@code info} 消息。</li>
	 * </ul>
	 * @param configAttributes 要处理的上下文配置属性
	 * @throws IllegalArgumentException 如果所提供的配置属性为 {@code null}，
	 * 或者所提供的配置属性同时包含资源位置和注解类
	 * @throws IllegalStateException 如果基于 XML 的加载器检测到默认配置类；
	 * 如果基于注解的加载器检测到默认资源位置；
	 * 如果两个候选加载器都未针对所提供的上下文配置检测到默认值；
	 * 或者如果两个候选加载器都针对所提供的上下文配置检测到默认值
	 */
	@Override
	public void processContextConfiguration(final ContextConfigurationAttributes configAttributes) {
		Assert.notNull(configAttributes, "configAttributes must not be null");
		Assert.isTrue(!(configAttributes.hasLocations() && configAttributes.hasClasses()),
				() -> String.format("Cannot process locations AND classes for context configuration %s: " +
						"configure one or the other, but not both.", configAttributes));

		// If the original locations or classes were not empty, there's no
		// need to bother with default detection checks; just let the
		// appropriate loader process the configuration.
		if (configAttributes.hasLocations()) {
			delegateProcessing(getXmlLoader(), configAttributes);
		}
		else if (configAttributes.hasClasses()) {
			delegateProcessing(getAnnotationConfigLoader(), configAttributes);
		}
		else {
			// Else attempt to detect defaults...

			// Let the XML loader process the configuration.
			delegateProcessing(getXmlLoader(), configAttributes);
			boolean xmlLoaderDetectedDefaults = configAttributes.hasLocations();

			if (xmlLoaderDetectedDefaults) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("%s detected default locations for context configuration %s.",
							name(getXmlLoader()), configAttributes));
				}
			}

			Assert.state(!configAttributes.hasClasses(), () -> String.format(
					"%s should NOT have detected default configuration classes for context configuration %s.",
					name(getXmlLoader()), configAttributes));

			// Now let the annotation config loader process the configuration.
			delegateProcessing(getAnnotationConfigLoader(), configAttributes);

			if (configAttributes.hasClasses()) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("%s detected default configuration classes for context configuration %s.",
							name(getAnnotationConfigLoader()), configAttributes));
				}
			}

			Assert.state(xmlLoaderDetectedDefaults || !configAttributes.hasLocations(), () -> String.format(
					"%s should NOT have detected default locations for context configuration %s.",
					name(getAnnotationConfigLoader()), configAttributes));

			if (configAttributes.hasLocations() && configAttributes.hasClasses()) {
				String msg = String.format(
						"Configuration error: both default locations AND default configuration classes " +
						"were detected for context configuration %s; configure one or the other, but not both.",
						configAttributes);
				logger.error(msg);
				throw new IllegalStateException(msg);
			}
		}
	}

	/**
	 * 委托给适当的候选 {@code SmartContextLoader} 来加载 {@link ApplicationContext}。
	 * <p>委托基于对 {@linkplain #getXmlLoader() XML 配置文件和 Groovy 脚本}以及 {@linkplain #getAnnotationConfigLoader() 注解类}的默认加载器实现的明确了解。具体而言，委托算法如下：
	 * <ul>
	 * <li>如果所提供的 {@code MergedContextConfiguration} 中的资源位置不为空且注解类为空，则基于 XML 的加载器将加载该 {@code ApplicationContext}。</li>
	 * <li>如果所提供的 {@code MergedContextConfiguration} 中的注解类不为空且资源位置为空，则基于注解的加载器将加载该 {@code ApplicationContext}。</li>
	 * </ul>
	 * @param mergedConfig 用于加载应用上下文的合并上下文配置
	 * @throws IllegalArgumentException 如果所提供的合并配置为 {@code null}
	 * @throws IllegalStateException 如果两个候选加载器都无法从所提供的合并上下文配置
	 * 加载 {@code ApplicationContext}
	 */
	@Override
	public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		Assert.notNull(mergedConfig, "MergedContextConfiguration must not be null");

		Assert.state(!(mergedConfig.hasLocations() && mergedConfig.hasClasses()), () -> String.format(
				"Neither %s nor %s supports loading an ApplicationContext from %s: " +
				"declare either 'locations' or 'classes' but not both.", name(getXmlLoader()),
				name(getAnnotationConfigLoader()), mergedConfig));

		SmartContextLoader[] candidates = {getXmlLoader(), getAnnotationConfigLoader()};
		for (SmartContextLoader loader : candidates) {
			// Determine if each loader can load a context from the mergedConfig. If it
			// can, let it; otherwise, keep iterating.
			if (supports(loader, mergedConfig)) {
				return delegateLoading(loader, mergedConfig);
			}
		}

		// If neither of the candidates supports the mergedConfig based on resources but
		// ACIs or customizers were declared, then delegate to the annotation config
		// loader.
		if (!mergedConfig.getContextInitializerClasses().isEmpty() || !mergedConfig.getContextCustomizers().isEmpty()) {
			return delegateLoading(getAnnotationConfigLoader(), mergedConfig);
		}

		// else...
		throw new IllegalStateException(String.format(
				"Neither %s nor %s was able to load an ApplicationContext from %s.",
				name(getXmlLoader()), name(getAnnotationConfigLoader()), mergedConfig));
	}


	private static void delegateProcessing(SmartContextLoader loader, ContextConfigurationAttributes configAttributes) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Delegating to %s to process context configuration %s.",
					name(loader), configAttributes));
		}
		loader.processContextConfiguration(configAttributes);
	}

	private static ApplicationContext delegateLoading(SmartContextLoader loader, MergedContextConfiguration mergedConfig)
			throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Delegating to %s to load context from %s.", name(loader), mergedConfig));
		}
		return loader.loadContext(mergedConfig);
	}

	private boolean supports(SmartContextLoader loader, MergedContextConfiguration mergedConfig) {
		if (loader == getAnnotationConfigLoader()) {
			return (mergedConfig.hasClasses() && !mergedConfig.hasLocations());
		}
		else {
			return (mergedConfig.hasLocations() && !mergedConfig.hasClasses());
		}
	}

	private static String name(SmartContextLoader loader) {
		return loader.getClass().getSimpleName();
	}

}
