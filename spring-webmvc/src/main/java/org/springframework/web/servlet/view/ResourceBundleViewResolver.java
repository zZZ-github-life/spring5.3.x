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

package org.springframework.web.servlet.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.View;

/**
 * {@link org.springframework.web.servlet.ViewResolver} 的一种实现，
 * 使用由 bundle 基名（basename）指定的 {@link ResourceBundle} 中的 bean 定义。
 *
 * <p>bundle 通常定义在位于 classpath 中的 properties 文件里。
 * 默认的 bundle 基名是 "views"。
 *
 * <p>此 {@code ViewResolver} 利用 {@link java.util.PropertyResourceBundle} 的默认支持，
 * 支持本地化的视图定义。例如，对于给定的 Locale "de_AT"，
 * 基名 "views" 将被解析为 classpath 资源 "views_de_AT.properties"、
 * "views_de.properties"、"views.properties"。
 *
 * <p>注意：此 {@code ViewResolver} 实现了 {@link Ordered} 接口，
 * 以便灵活参与 {@code ViewResolver} 链。例如，可以通过此 {@code ViewResolver}
 * 定义一些特殊视图（将其 "order" 值设为 0），而所有其余视图则可以由
 * {@link UrlBasedViewResolver} 解析。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see java.util.ResourceBundle#getBundle
 * @see java.util.PropertyResourceBundle
 * @see UrlBasedViewResolver
 * @see BeanNameViewResolver
 * @deprecated as of 5.3, in favor of Spring's common view resolver variants
 * and/or custom resolver implementations
 */
@Deprecated
public class ResourceBundleViewResolver extends AbstractCachingViewResolver
		implements Ordered, InitializingBean, DisposableBean {

	/** 如果未提供其他基名，则使用的默认基名。 */
	public static final String DEFAULT_BASENAME = "views";


	private String[] basenames = new String[] {DEFAULT_BASENAME};

	private ClassLoader bundleClassLoader = Thread.currentThread().getContextClassLoader();

	@Nullable
	private String defaultParentView;

	@Nullable
	private Locale[] localesToInitialize;

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	/* Locale -> BeanFactory */
	private final Map<Locale, BeanFactory> localeCache = new HashMap<>();

	/* List of ResourceBundle -> BeanFactory */
	private final Map<List<ResourceBundle>, ConfigurableApplicationContext> bundleCache = new HashMap<>();


	/**
	 * 设置单个基名，遵循 {@link java.util.ResourceBundle} 的约定。
	 * 默认为 "views"。
	 * <p>{@code ResourceBundle} 支持不同的 locale 后缀。例如，
	 * 基名 "views" 可以映射到 {@code ResourceBundle} 文件
	 * "views"、"views_en_au" 和 "views_de"。
	 * <p>注意：ResourceBundle 名称实际上就是 classpath 位置：因此，
	 * JDK 标准的 ResourceBundle 会将点号视为包分隔符。
	 * 这意味着 "test.theme" 实际上等价于 "test/theme"，
	 * 这与编程方式使用 {@code java.util.ResourceBundle} 时的情况相同。
	 * @see #setBasenames
	 * @see ResourceBundle#getBundle(String)
	 * @see ResourceBundle#getBundle(String, Locale)
	 */
	public void setBasename(String basename) {
		setBasenames(basename);
	}

	/**
	 * 设置基名数组，每个基名都遵循 {@link java.util.ResourceBundle} 的约定。
	 * 默认为单个基名 "views"。
	 * <p>{@code ResourceBundle} 支持不同的 locale 后缀。例如，
	 * 基名 "views" 可以映射到 {@code ResourceBundle} 文件
	 * "views"、"views_en_au" 和 "views_de"。
	 * <p>在解析消息代码时，将按顺序检查关联的资源 bundle。
	 * 请注意，由于是按顺序查找，<i>前一个</i>资源 bundle 中的消息定义
	 * 会覆盖后一个 bundle 中的定义。
	 * <p>注意：ResourceBundle 名称实际上就是 classpath 位置：因此，
	 * JDK 标准的 ResourceBundle 会将点号视为包分隔符。
	 * 这意味着 "test.theme" 实际上等价于 "test/theme"，
	 * 这与编程方式使用 {@code java.util.ResourceBundle} 时的情况相同。
	 * @see #setBasename
	 * @see ResourceBundle#getBundle(String)
	 * @see ResourceBundle#getBundle(String, Locale)
	 */
	public void setBasenames(String... basenames) {
		this.basenames = basenames;
	}

	/**
	 * 设置用于加载资源 bundle 的 {@link ClassLoader}。
	 * 默认为线程上下文 {@code ClassLoader}。
	 */
	public void setBundleClassLoader(ClassLoader classLoader) {
		this.bundleClassLoader = classLoader;
	}

	/**
	 * 返回用于加载资源 bundle 的 {@link ClassLoader}。
	 * <p>默认为指定的 bundle {@code ClassLoader}，
	 * 通常是线程上下文 {@code ClassLoader}。
	 */
	protected ClassLoader getBundleClassLoader() {
		return this.bundleClassLoader;
	}

	/**
	 * 设置 {@code ResourceBundle} 中所定义视图的默认父视图。
	 * <p>这样可以避免在 bundle 中重复定义 "yyy1.(parent)=xxx"、"yyy2.(parent)=xxx"，
	 * 尤其是在所有已定义视图共享同一父视图时。
	 * <p>父视图通常会定义视图类和公共属性。
	 * 具体视图随后可以仅由 URL 定义组成：
	 * 例如 "yyy1.url=/my.jsp"、"yyy2.url=/your.jsp"。
	 * <p>定义了自己的父视图或自带 class 的视图定义仍然可以覆盖此设置。
	 * 严格来说，默认父视图设置不适用于自带 class 的 bean 定义，
	 * 这条规则是出于向后兼容的考虑。不过它仍然符合典型的使用场景。
	 */
	public void setDefaultParentView(String defaultParentView) {
		this.defaultParentView = defaultParentView;
	}

	/**
	 * 指定需要立即初始化的 Locale，而不是等到实际访问时才延迟初始化。
	 * <p>这样可以预初始化常用的 Locale，提前检查这些 Locale 的视图配置。
	 */
	public void setLocalesToInitialize(Locale... localesToInitialize) {
		this.localesToInitialize = localesToInitialize;
	}

	/**
	 * 指定此 ViewResolver bean 的 order 值。
	 * <p>默认值为 {@code Ordered.LOWEST_PRECEDENCE}，表示不排序。
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 如有必要，立即初始化各 Locale。
	 * @see #setLocalesToInitialize
	 */
	@Override
	public void afterPropertiesSet() throws BeansException {
		if (this.localesToInitialize != null) {
			for (Locale locale : this.localesToInitialize) {
				initFactory(locale);
			}
		}
	}


	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		BeanFactory factory = initFactory(locale);
		try {
			return factory.getBean(viewName, View.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Allow for ViewResolver chaining...
			return null;
		}
	}

	/**
	 * 针对给定的 {@link Locale}，从 {@code ResourceBundle} 初始化视图 {@link BeanFactory}。
	 * <p>由于会被并行线程访问，因此使用同步。
	 * @param locale 目标 {@code Locale}
	 * @return 给定 Locale 的 View 工厂
	 * @throws BeansException 如果发生初始化错误
	 */
	protected synchronized BeanFactory initFactory(Locale locale) throws BeansException {
		// Try to find cached factory for Locale:
		// Have we already encountered that Locale before?
		if (isCache()) {
			BeanFactory cachedFactory = this.localeCache.get(locale);
			if (cachedFactory != null) {
				return cachedFactory;
			}
		}

		// Build list of ResourceBundle references for Locale.
		List<ResourceBundle> bundles = new ArrayList<>(this.basenames.length);
		for (String basename : this.basenames) {
			bundles.add(getBundle(basename, locale));
		}

		// Try to find cached factory for ResourceBundle list:
		// even if Locale was different, same bundles might have been found.
		if (isCache()) {
			BeanFactory cachedFactory = this.bundleCache.get(bundles);
			if (cachedFactory != null) {
				this.localeCache.put(locale, cachedFactory);
				return cachedFactory;
			}
		}

		// Create child ApplicationContext for views.
		GenericWebApplicationContext factory = new GenericWebApplicationContext();
		factory.setParent(getApplicationContext());
		factory.setServletContext(getServletContext());

		// Load bean definitions from resource bundle.
		org.springframework.beans.factory.support.PropertiesBeanDefinitionReader reader =
				new org.springframework.beans.factory.support.PropertiesBeanDefinitionReader(factory);
		reader.setDefaultParentBean(this.defaultParentView);
		for (ResourceBundle bundle : bundles) {
			reader.registerBeanDefinitions(bundle);
		}

		factory.refresh();

		// Cache factory for both Locale and ResourceBundle list.
		if (isCache()) {
			this.localeCache.put(locale, factory);
			this.bundleCache.put(bundles, factory);
		}

		return factory;
	}

	/**
	 * 获取给定基名和 {@link Locale} 对应的资源 bundle。
	 * @param basename 要查找的基名
	 * @param locale 要查找的 {@code Locale}
	 * @return 相应的 {@code ResourceBundle}
	 * @throws MissingResourceException 如果未找到匹配的资源包
	 * @see ResourceBundle#getBundle(String, Locale, ClassLoader)
	 */
	protected ResourceBundle getBundle(String basename, Locale locale) throws MissingResourceException {
		return ResourceBundle.getBundle(basename, locale, getBundleClassLoader());
	}


	/**
	 * 在上下文关闭时关闭 bundle 视图工厂。
	 */
	@Override
	public void destroy() throws BeansException {
		for (ConfigurableApplicationContext factory : this.bundleCache.values()) {
			factory.close();
		}
		this.localeCache.clear();
		this.bundleCache.clear();
	}

}
