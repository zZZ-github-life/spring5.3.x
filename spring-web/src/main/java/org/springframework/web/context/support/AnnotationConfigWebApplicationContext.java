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

package org.springframework.web.context.support;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ContextLoader;

/**
 * {@link org.springframework.web.context.WebApplicationContext WebApplicationContext}
 * 实现，接受<em>组件类</em>作为输入 &mdash; 特别是
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * 类，也包括普通的 {@link org.springframework.stereotype.Component @Component}
 * 类，以及使用 {@code javax.inject} 注解的符合 JSR-330 的类。
 *
 * <p>支持逐个注册类（将类名指定为配置位置），也支持通过类路径扫描注册（将基础包指定为配置位置）。
 *
 * <p>它本质上相当于 Web 环境中的
 * {@link org.springframework.context.annotation.AnnotationConfigApplicationContext
 * AnnotationConfigApplicationContext}。不过，与
 * {@code AnnotationConfigApplicationContext} 不同，此类并不扩展
 * {@link org.springframework.context.support.GenericApplicationContext
 * GenericApplicationContext}，因此不提供 {@code GenericApplicationContext} 中
 * 一些便捷的 {@code registerBean(...)} 方法。如果希望在 Web 环境中向
 * {@code GenericApplicationContext} 注册带注解的<em>组件类</em>，可以使用
 * {@code GenericWebApplicationContext} 配合
 * {@link org.springframework.context.annotation.AnnotatedBeanDefinitionReader
 * AnnotatedBeanDefinitionReader}。有关详细信息和示例，请参见
 * {@link GenericWebApplicationContext} 的 Javadoc。
 *
 * <p>要使用此应用上下文，必须将 ContextLoader 的
 * {@linkplain ContextLoader#CONTEXT_CLASS_PARAM "contextClass"} context-param 和/或
 * FrameworkServlet 的 "contextClass" init-param 设置为此类的全限定名。
 *
 * <p>自 Spring 3.1 起，在使用
 * {@link org.springframework.web.WebApplicationInitializer WebApplicationInitializer}
 * 作为 {@code web.xml} 的基于代码的替代方案时，也可以直接实例化此类并将其注入
 * Spring 的 {@code DispatcherServlet} 或 {@code ContextLoaderListener}。
 * 有关详细信息和用法示例，请参见其 Javadoc。
 *
 * <p>与 {@link XmlWebApplicationContext} 不同，这里不假定任何默认的配置类位置。
 * 相反，必须设置 {@link ContextLoader} 的
 * {@linkplain ContextLoader#CONFIG_LOCATION_PARAM "contextConfigLocation"}
 * context-param 和/或 FrameworkServlet 的 "contextConfigLocation" init-param。
 * 参数值可以同时包含全限定类名和用于扫描组件的基础包。有关这些位置如何被处理的
 * 确切细节，请参见 {@link #loadBeanDefinitions}。
 *
 * <p>作为设置 "contextConfigLocation" 参数的替代方案，用户可以实现一个
 * {@link org.springframework.context.ApplicationContextInitializer
 * ApplicationContextInitializer} 并设置
 * {@linkplain ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM "contextInitializerClasses"}
 * context-param / init-param。在这种情况下，用户应优先使用 {@link #refresh()}
 * 和 {@link #scan(String...)} 方法，而不是主要供 {@code ContextLoader} 使用的
 * {@link #setConfigLocation(String)} 方法。
 *
 * <p>注意：在有多个 {@code @Configuration} 类的情况下，后定义的 {@code @Bean} 定义将覆盖先前加载
 * 文件中定义的 bean。可以利用这一点，通过一个额外的 {@code @Configuration} 类来有意覆盖某些 bean
 * 定义。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 * @see org.springframework.web.context.support.GenericWebApplicationContext
 */
public class AnnotationConfigWebApplicationContext extends AbstractRefreshableWebApplicationContext
		implements AnnotationConfigRegistry {

	@Nullable
	private BeanNameGenerator beanNameGenerator;

	@Nullable
	private ScopeMetadataResolver scopeMetadataResolver;

	private final Set<Class<?>> componentClasses = new LinkedHashSet<>();

	private final Set<String> basePackages = new LinkedHashSet<>();


	/**
	 * 为 {@link AnnotatedBeanDefinitionReader} 和/或
	 * {@link ClassPathBeanDefinitionScanner} 设置自定义的 {@link BeanNameGenerator}。
	 * <p>默认值为 {@link org.springframework.context.annotation.AnnotationBeanNameGenerator}。
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = beanNameGenerator;
	}

	/**
	 * 返回用于 {@link AnnotatedBeanDefinitionReader} 和/或 {@link ClassPathBeanDefinitionScanner} 的
	 * 自定义 {@link BeanNameGenerator}（如果有）。
	 */
	@Nullable
	protected BeanNameGenerator getBeanNameGenerator() {
		return this.beanNameGenerator;
	}

	/**
	 * 为 {@link AnnotatedBeanDefinitionReader} 和/或 {@link ClassPathBeanDefinitionScanner}
	 * 设置自定义的 {@link ScopeMetadataResolver}。
	 * <p>默认值为 {@link org.springframework.context.annotation.AnnotationScopeMetadataResolver}。
	 * @see AnnotatedBeanDefinitionReader#setScopeMetadataResolver
	 * @see ClassPathBeanDefinitionScanner#setScopeMetadataResolver
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver = scopeMetadataResolver;
	}

	/**
	 * 返回用于 {@link AnnotatedBeanDefinitionReader} 和/或 {@link ClassPathBeanDefinitionScanner} 的
	 * 自定义 {@link ScopeMetadataResolver}（如果有）。
	 */
	@Nullable
	protected ScopeMetadataResolver getScopeMetadataResolver() {
		return this.scopeMetadataResolver;
	}


	/**
	 * 注册一个或多个要处理的组件类。
	 * <p>注意，必须调用 {@link #refresh()} 才能让上下文完整处理新的类。
	 * @param componentClasses 一个或多个组件类，例如 {@link org.springframework.context.annotation.Configuration @Configuration} 类
	 * @see #scan(String...)
	 * @see #loadBeanDefinitions(DefaultListableBeanFactory)
	 * @see #setConfigLocation(String)
	 * @see #refresh()
	 */
	@Override
	public void register(Class<?>... componentClasses) {
		Assert.notEmpty(componentClasses, "At least one component class must be specified");
		Collections.addAll(this.componentClasses, componentClasses);
	}

	/**
	 * 在指定的基础包中执行扫描。
	 * <p>注意，必须调用 {@link #refresh()} 才能让上下文完整处理新的类。
	 * @param basePackages 要检查组件类的包
	 * @see #loadBeanDefinitions(DefaultListableBeanFactory)
	 * @see #register(Class...)
	 * @see #setConfigLocation(String)
	 * @see #refresh()
	 */
	@Override
	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		Collections.addAll(this.basePackages, basePackages);
	}


	/**
	 * 为 {@link #register(Class...)} 指定的任何类注册
	 * {@link org.springframework.beans.factory.config.BeanDefinition}，
	 * 并扫描 {@link #scan(String...)} 指定的任何包。
	 * <p>对于 {@link #setConfigLocation(String)} 或
	 * {@link #setConfigLocations(String[])} 指定的任何值，会首先尝试将每个位置作为类加载，
	 * 如果类加载成功则注册一个 {@code BeanDefinition}；如果类加载失败（即抛出
	 * {@code ClassNotFoundException}），则假定该值是一个包，并尝试在其中扫描组件类。
	 * <p>启用默认的注解配置后处理器集合，从而可以使用 {@code @Autowired}、{@code @Required}
	 * 及相关注解。
	 * <p>除非在构造型注解中提供了 {@code value} 属性，否则配置类的 bean 定义会使用生成的
	 * bean 定义名称注册。
	 * @param beanFactory 用于加载 Bean 定义的 Bean 工厂
	 * @see #register(Class...)
	 * @see #scan(String...)
	 * @see #setConfigLocation(String)
	 * @see #setConfigLocations(String[])
	 * @see AnnotatedBeanDefinitionReader
	 * @see ClassPathBeanDefinitionScanner
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
		AnnotatedBeanDefinitionReader reader = getAnnotatedBeanDefinitionReader(beanFactory);
		ClassPathBeanDefinitionScanner scanner = getClassPathBeanDefinitionScanner(beanFactory);

		BeanNameGenerator beanNameGenerator = getBeanNameGenerator();
		if (beanNameGenerator != null) {
			reader.setBeanNameGenerator(beanNameGenerator);
			scanner.setBeanNameGenerator(beanNameGenerator);
			beanFactory.registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
		}

		ScopeMetadataResolver scopeMetadataResolver = getScopeMetadataResolver();
		if (scopeMetadataResolver != null) {
			reader.setScopeMetadataResolver(scopeMetadataResolver);
			scanner.setScopeMetadataResolver(scopeMetadataResolver);
		}

		if (!this.componentClasses.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Registering component classes: [" +
						StringUtils.collectionToCommaDelimitedString(this.componentClasses) + "]");
			}
			reader.register(ClassUtils.toClassArray(this.componentClasses));
		}

		if (!this.basePackages.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Scanning base packages: [" +
						StringUtils.collectionToCommaDelimitedString(this.basePackages) + "]");
			}
			scanner.scan(StringUtils.toStringArray(this.basePackages));
		}

		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				try {
					Class<?> clazz = ClassUtils.forName(configLocation, getClassLoader());
					if (logger.isTraceEnabled()) {
						logger.trace("Registering [" + configLocation + "]");
					}
					reader.register(clazz);
				}
				catch (ClassNotFoundException ex) {
					if (logger.isTraceEnabled()) {
						logger.trace("Could not load class for config location [" + configLocation +
								"] - trying package scan. " + ex);
					}
					int count = scanner.scan(configLocation);
					if (count == 0 && logger.isDebugEnabled()) {
						logger.debug("No component classes found for specified class/package [" + configLocation + "]");
					}
				}
			}
		}
	}


	/**
	 * 为给定的 bean 工厂构建一个 {@link AnnotatedBeanDefinitionReader}。
	 * <p>应使用 {@code Environment}（如果需要）对其预配置，但此时不要配置 {@code BeanNameGenerator}
	 * 或 {@code ScopeMetadataResolver}。
	 * @param beanFactory 用于加载 Bean 定义的 Bean 工厂
	 * @since 4.1.9
	 * @see #getEnvironment()
	 * @see #getBeanNameGenerator()
	 * @see #getScopeMetadataResolver()
	 */
	protected AnnotatedBeanDefinitionReader getAnnotatedBeanDefinitionReader(DefaultListableBeanFactory beanFactory) {
		return new AnnotatedBeanDefinitionReader(beanFactory, getEnvironment());
	}

	/**
	 * 为给定的 bean 工厂构建一个 {@link ClassPathBeanDefinitionScanner}。
	 * <p>应使用 {@code Environment}（如果需要）对其预配置，但此时不要配置 {@code BeanNameGenerator}
	 * 或 {@code ScopeMetadataResolver}。
	 * @param beanFactory 用于加载 Bean 定义的 Bean 工厂
	 * @since 4.1.9
	 * @see #getEnvironment()
	 * @see #getBeanNameGenerator()
	 * @see #getScopeMetadataResolver()
	 */
	protected ClassPathBeanDefinitionScanner getClassPathBeanDefinitionScanner(DefaultListableBeanFactory beanFactory) {
		return new ClassPathBeanDefinitionScanner(beanFactory, true, getEnvironment());
	}

}
