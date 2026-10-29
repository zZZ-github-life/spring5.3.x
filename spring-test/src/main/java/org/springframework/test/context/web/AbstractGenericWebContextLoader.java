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

package org.springframework.test.context.web;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * {@link AbstractContextLoader} 的抽象通用扩展，用于加载 {@link GenericWebApplicationContext}。
 * <p>如果具体子类的实例通过 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} SPI 调用，则将从提供给 {@link #loadContext(MergedContextConfiguration)} 的 {@link MergedContextConfiguration} 加载上下文。在这种情况下，{@code SmartContextLoader} 将决定是从<em>locations</em> 还是从<em>annotated classes</em> 加载上下文。请注意，{@code AbstractGenericWebContextLoader} 不支持旧版 {@link org.springframework.test.context.ContextLoader ContextLoader} SPI 中的 {@code loadContext(String... locations)} 方法。
 * <p>具体子类必须提供 {@link #loadBeanDefinitions} 的适当实现。
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.2
 * @see #loadContext(MergedContextConfiguration)
 * @see #loadContext(String...)
 */
public abstract class AbstractGenericWebContextLoader extends AbstractContextLoader {

	protected static final Log logger = LogFactory.getLog(AbstractGenericWebContextLoader.class);


	// SmartContextLoader

	/**
	 * 从所提供的 {@link MergedContextConfiguration} 加载 Spring {@link WebApplicationContext}。
	 * <p>实现细节：
	 * <ul>
	 * <li>调用 {@link #validateMergedContextConfiguration(WebMergedContextConfiguration)}，以允许子类在继续之前验证所提供的配置。</li>
	 * <li>创建 {@link GenericWebApplicationContext} 实例。</li>
	 * <li>如果所提供的 {@code MergedContextConfiguration} 引用了{@linkplain MergedContextConfiguration#getParent() 父配置}，则获取相应的 {@link MergedContextConfiguration#getParentApplicationContext() ApplicationContext}，并将其{@linkplain GenericWebApplicationContext#setParent(ApplicationContext) 设置为父上下文}，作为此方法所创建上下文的父级。</li>
	 * <li>委托 {@link #configureWebResources} 创建 {@link MockServletContext} 并将其设置到 {@code WebApplicationContext} 中。</li>
	 * <li>调用 {@link #prepareContext}，以允许在加载 bean 定义之前自定义上下文。</li>
	 * <li>调用 {@link #customizeBeanFactory}，以允许自定义上下文的 {@code DefaultListableBeanFactory}。</li>
	 * <li>委托 {@link #loadBeanDefinitions}，从所提供的 {@code MergedContextConfiguration} 中的位置或类填充上下文。</li>
	 * <li>委托 {@link AnnotationConfigUtils} 来{@linkplain AnnotationConfigUtils#registerAnnotationConfigProcessors 注册}注解配置处理器。</li>
	 * <li>调用 {@link #customizeContext}，以允许在刷新上下文之前自定义上下文。</li>
	 * <li>{@link ConfigurableApplicationContext#refresh 刷新}上下文，并为其注册 JVM 关闭钩子。</li>
	 * </ul>
	 * @return a new web application context
	 * @see org.springframework.test.context.SmartContextLoader#loadContext(MergedContextConfiguration)
	 * @see GenericWebApplicationContext
	 */
	@Override
	public final ConfigurableApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		Assert.isTrue(mergedConfig instanceof WebMergedContextConfiguration,
				() -> String.format("Cannot load WebApplicationContext from non-web merged context configuration %s. " +
						"Consider annotating your test class with @WebAppConfiguration.", mergedConfig));

		WebMergedContextConfiguration webMergedConfig = (WebMergedContextConfiguration) mergedConfig;

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Loading WebApplicationContext for merged context configuration %s.",
				webMergedConfig));
		}

		validateMergedContextConfiguration(webMergedConfig);

		GenericWebApplicationContext context = createContext();

		ApplicationContext parent = mergedConfig.getParentApplicationContext();
		if (parent != null) {
			context.setParent(parent);
		}
		configureWebResources(context, webMergedConfig);
		prepareContext(context, webMergedConfig);
		customizeBeanFactory(context.getDefaultListableBeanFactory(), webMergedConfig);
		loadBeanDefinitions(context, webMergedConfig);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		customizeContext(context, webMergedConfig);
		context.refresh();
		context.registerShutdownHook();
		return context;
	}

	/**
	 * 针对此上下文加载器支持的功能验证所提供的 {@link WebMergedContextConfiguration}。
	 * <p>默认实现是<em>空操作</em>，但可由子类根据情况重写。
	 * @param mergedConfig the merged configuration to validate
	 * @throws IllegalStateException if the supplied configuration is not valid
	 * for this context loader
	 * @since 4.0.4
	 */
	protected void validateMergedContextConfiguration(WebMergedContextConfiguration mergedConfig) {
		// no-op
	}

	/**
	 * 用于创建此 {@code ContextLoader} 所使用的 {@link GenericWebApplicationContext} 的工厂方法。
	 * <p>默认实现使用默认构造函数创建 {@code GenericWebApplicationContext}。可以重写此方法——例如，使用自定义上下文子类，或创建具有自定义 {@link DefaultListableBeanFactory} 实现的 {@code GenericWebApplicationContext}。
	 * @return a newly instantiated {@code GenericWebApplicationContext}
	 * @since 5.3.23
	 */
	protected GenericWebApplicationContext createContext() {
		return new GenericWebApplicationContext();
	}

	/**
	 * 为所提供的 Web 应用程序上下文（WAC）配置 Web 资源。
	 * <h4>实现细节</h4>
	 * <p>如果所提供的 WAC 没有父级，或者其父级不是 WAC，则所提供的 WAC 将被配置为根 WAC（请参见下方的“<em>根 WAC 配置</em>”）。
	 * <p>否则，将遍历所提供的 WAC 的上下文层次结构以找到最顶层的 WAC（即根）；并将根 WAC 的 {@link ServletContext} 设置为所提供的 WAC 的 {@code ServletContext}。
	 * <h4>根 WAC 配置</h4>
	 * <ul>
	 * <li>从所提供的 {@code WebMergedContextConfiguration} 中获取资源基路径。</li>
	 * <li>为 {@link MockServletContext} 实例化一个 {@link ResourceLoader}：如果资源基路径以“{@code classpath:}”为前缀，则使用 {@link DefaultResourceLoader}；否则使用 {@link FileSystemResourceLoader}。</li>
	 * <li>使用资源基路径和资源加载器创建 {@code MockServletContext}。</li>
	 * <li>然后将所提供的 {@link GenericWebApplicationContext} 以 {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} 为键存储到 {@code MockServletContext} 中。</li>
	 * <li>最后，将 {@code MockServletContext} 设置到 {@code WebApplicationContext} 中。</li>
	 * </ul>
	 * @param context the web application context for which to configure the web resources
	 * @param webMergedConfig the merged context configuration to use to load the web application context
	 */
	protected void configureWebResources(GenericWebApplicationContext context,
			WebMergedContextConfiguration webMergedConfig) {

		ApplicationContext parent = context.getParent();

		// If the WebApplicationContext has no parent or the parent is not a WebApplicationContext,
		// set the current context as the root WebApplicationContext:
		if (!(parent instanceof WebApplicationContext)) {
			String resourceBasePath = webMergedConfig.getResourceBasePath();
			ResourceLoader resourceLoader = (resourceBasePath.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX) ?
					new DefaultResourceLoader() : new FileSystemResourceLoader());
			ServletContext servletContext = new MockServletContext(resourceBasePath, resourceLoader);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
			context.setServletContext(servletContext);
		}
		else {
			ServletContext servletContext = null;
			// Find the root WebApplicationContext
			while (parent != null) {
				if (parent instanceof WebApplicationContext && !(parent.getParent() instanceof WebApplicationContext)) {
					servletContext = ((WebApplicationContext) parent).getServletContext();
					break;
				}
				parent = parent.getParent();
			}
			Assert.state(servletContext != null, "Failed to find root WebApplicationContext in the context hierarchy");
			context.setServletContext(servletContext);
		}
	}

	/**
	 * 自定义此上下文加载器创建的 {@code WebApplicationContext} 的内部 bean 工厂。
	 * <p>默认实现为空，但可在子类中重写以自定义 {@code DefaultListableBeanFactory} 的标准设置。
	 * @param beanFactory the bean factory created by this context loader
	 * @param webMergedConfig the merged context configuration to use to load the
	 * web application context
	 * @see #loadContext(MergedContextConfiguration)
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see DefaultListableBeanFactory#setAllowCircularReferences
	 * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 */
	protected void customizeBeanFactory(
			DefaultListableBeanFactory beanFactory, WebMergedContextConfiguration webMergedConfig) {
	}

	/**
	 * 从所提供的 {@code WebMergedContextConfiguration} 中的位置或类加载 bean 定义到所提供的 {@link GenericWebApplicationContext context} 中。
	 * <p>具体子类必须提供适当的实现。
	 * @param context the context into which the bean definitions should be loaded
	 * @param webMergedConfig the merged context configuration to use to load the
	 * web application context
	 * @see #loadContext(MergedContextConfiguration)
	 */
	protected abstract void loadBeanDefinitions(
			GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig);

	/**
	 * 在 bean 定义已加载到上下文中<i>之后</i>但在上下文刷新<i>之前</i>，自定义此上下文加载器创建的 {@link GenericWebApplicationContext}。
	 * <p>默认实现仅委托给 {@link AbstractContextLoader#customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)}。
	 * @param context the newly created web application context
	 * @param webMergedConfig the merged context configuration to use to load the
	 * web application context
	 * @see #loadContext(MergedContextConfiguration)
	 * @see #customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)
	 */
	protected void customizeContext(
			GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig) {

		super.customizeContext(context, webMergedConfig);
	}


	// ContextLoader

	/**
	 * {@code AbstractGenericWebContextLoader} 应作为 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} 使用，而不是旧版 {@link org.springframework.test.context.ContextLoader ContextLoader}。因此，不支持此方法。
	 * @throws UnsupportedOperationException in this implementation
	 * @see org.springframework.test.context.ContextLoader#loadContext(java.lang.String[])
	 */
	@Override
	public final ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException(
				"AbstractGenericWebContextLoader does not support the loadContext(String... locations) method");
	}

}
