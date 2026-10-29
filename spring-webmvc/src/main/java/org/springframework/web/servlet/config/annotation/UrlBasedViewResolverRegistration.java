/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.Map;

import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * 辅助配置 {@link org.springframework.web.servlet.view.UrlBasedViewResolver}。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class UrlBasedViewResolverRegistration {

	protected final UrlBasedViewResolver viewResolver;


	public UrlBasedViewResolverRegistration(UrlBasedViewResolver viewResolver) {
		this.viewResolver = viewResolver;
	}


	protected UrlBasedViewResolver getViewResolver() {
		return this.viewResolver;
	}

	/**
	 * 设置构建 URL 时添加到视图名称前面的前缀。
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setPrefix
	 */
	public UrlBasedViewResolverRegistration prefix(String prefix) {
		this.viewResolver.setPrefix(prefix);
		return this;
	}

	/**
	 * 设置构建 URL 时追加到视图名称后面的后缀。
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setSuffix
	 */
	public UrlBasedViewResolverRegistration suffix(String suffix) {
		this.viewResolver.setSuffix(suffix);
		return this;
	}

	/**
	 * 设置应用于创建视图的视图类。
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setViewClass
	 */
	public UrlBasedViewResolverRegistration viewClass(Class<?> viewClass) {
		this.viewResolver.setViewClass(viewClass);
		return this;
	}

	/**
	 * 设置此视图解析器可以处理的视图名称（或名称模式）。视图名称可以包含简单的通配符，
	 * 因此 'my*'、'*Report' 和 '*Repo*' 都能匹配视图名称 'myReport'。
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setViewNames
	 */
	public UrlBasedViewResolverRegistration viewNames(String... viewNames) {
		this.viewResolver.setViewNames(viewNames);
		return this;
	}

	/**
	 * 设置静态属性，这些属性将被添加到由该视图解析器解析的所有视图的每个请求模型中。
	 * 这样可以设置任意类型的属性值，例如 bean 引用。
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setAttributesMap
	 */
	public UrlBasedViewResolverRegistration attributes(Map<String, ?> attributes) {
		this.viewResolver.setAttributesMap(attributes);
		return this;
	}

	/**
	 * 指定视图缓存的最大条目数。
	 * 默认为 1024。
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setCache(boolean)
	 */
	public UrlBasedViewResolverRegistration cacheLimit(int cacheLimit) {
		this.viewResolver.setCacheLimit(cacheLimit);
		return this;
	}

	/**
	 * 启用或禁用缓存。
	 * <p>这分别等同于将 {@link #cacheLimit "cacheLimit"} 属性设置为默认上限（1024）或 0。
	 * <p>默认为 "true"：启用缓存。
	 * 仅在调试和开发时禁用。
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setCache(boolean)
	 */
	public UrlBasedViewResolverRegistration cache(boolean cache) {
		this.viewResolver.setCache(cache);
		return this;
	}

}


