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

import java.util.Locale;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.View;

/**
 * {@link org.springframework.web.servlet.ViewResolver} 的一种实现，
 * 使用由资源位置指定的专用 XML 文件中的 bean 定义作为视图定义。
 * 该文件通常位于 WEB-INF 目录下；默认为 "/WEB-INF/views.xml"。
 *
 * <p>此 {@code ViewResolver} 不支持在其定义资源层面进行国际化。
 * 如果需要按 locale 应用不同的视图资源，请考虑使用 {@link ResourceBundleViewResolver}。
 *
 * <p>注意：此 {@code ViewResolver} 实现了 {@link Ordered} 接口，
 * 以便灵活参与 {@code ViewResolver} 链。例如，可以通过此 {@code ViewResolver}
 * 定义一些特殊视图（将其 "order" 值设为 0），而所有其余视图则可以由
 * {@link UrlBasedViewResolver} 解析。
 *
 * @author Juergen Hoeller
 * @since 18.06.2003
 * @see org.springframework.context.ApplicationContext#getResource
 * @see UrlBasedViewResolver
 * @see BeanNameViewResolver
 * @deprecated as of 5.3, in favor of Spring's common view resolver variants
 * and/or custom resolver implementations
 */
@Deprecated
public class XmlViewResolver extends AbstractCachingViewResolver
		implements Ordered, InitializingBean, DisposableBean {

	/** 如果未提供其他位置，则使用的默认位置。 */
	public static final String DEFAULT_LOCATION = "/WEB-INF/views.xml";


	@Nullable
	private Resource location;

	@Nullable
	private ConfigurableApplicationContext cachedFactory;

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered


	/**
	 * 设置定义视图 bean 的 XML 文件的位置。
	 * <p>默认为 "/WEB-INF/views.xml"。
	 * @param location XML 文件的位置。
	 */
	public void setLocation(Resource location) {
		this.location = location;
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
	 * 从 XML 文件预初始化工厂。
	 * 仅在启用缓存时有效。
	 */
	@Override
	public void afterPropertiesSet() throws BeansException {
		if (isCache()) {
			initFactory();
		}
	}


	/**
	 * 此实现仅返回视图名称，因为 XmlViewResolver 不支持本地化解析。
	 */
	@Override
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName;
	}

	@Override
	protected View loadView(String viewName, Locale locale) throws BeansException {
		BeanFactory factory = initFactory();
		try {
			return factory.getBean(viewName, View.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Allow for ViewResolver chaining...
			return null;
		}
	}

	/**
	 * 从 XML 文件初始化视图 bean 工厂。
	 * 由于会被并行线程访问，因此使用同步。
	 * @throws BeansException 如果发生初始化错误
	 */
	protected synchronized BeanFactory initFactory() throws BeansException {
		if (this.cachedFactory != null) {
			return this.cachedFactory;
		}

		ApplicationContext applicationContext = obtainApplicationContext();

		Resource actualLocation = this.location;
		if (actualLocation == null) {
			actualLocation = applicationContext.getResource(DEFAULT_LOCATION);
		}

		// Create child ApplicationContext for views.
		GenericWebApplicationContext factory = new GenericWebApplicationContext();
		factory.setParent(applicationContext);
		factory.setServletContext(getServletContext());

		// Load XML resource with context-aware entity resolver.
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.setEnvironment(applicationContext.getEnvironment());
		reader.setEntityResolver(new ResourceEntityResolver(applicationContext));
		reader.loadBeanDefinitions(actualLocation);

		factory.refresh();

		if (isCache()) {
			this.cachedFactory = factory;
		}
		return factory;
	}


	/**
	 * 在上下文关闭时关闭视图 bean 工厂。
	 */
	@Override
	public void destroy() throws BeansException {
		if (this.cachedFactory != null) {
			this.cachedFactory.close();
		}
	}

}
