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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

/**
 * 辅助注册预先配置了状态码和/或视图的简单自动化控制器。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class ViewControllerRegistry {

	@Nullable
	private ApplicationContext applicationContext;

	private final List<ViewControllerRegistration> registrations = new ArrayList<>(4);

	private final List<RedirectViewControllerRegistration> redirectRegistrations = new ArrayList<>(10);

	private int order = 1;


	/**
	 * 带有 {@link ApplicationContext} 的类构造函数。
	 * @since 4.3.12
	 */
	public ViewControllerRegistry(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	/**
	 * 将 URL 路径或模式映射到视图控制器，以使用配置的状态码和视图渲染响应。
	 * <p>支持诸如 {@code "/admin/**"} 或 {@code "/articles/{articlename:\\w+}"} 之类的模式。
	 * 关于模式语法，当解析模式被
	 * {@link PathMatchConfigurer#setPatternParser 启用}时请参见 {@link PathPattern}，
	 * 否则请参见 {@link AntPathMatcher}。两者语法大体相同，
	 * 但 {@link PathPattern} 更贴合 Web 用途且更高效。
	 * <p><strong>注意：</strong>如果某个 {@code @RequestMapping} 方法映射到了某个 URL
	 * （无论何种 HTTP 方法），则视图控制器无法处理同一个 URL。
	 * 因此建议避免将 URL 处理拆分到注解控制器和视图控制器中。
	 */
	public ViewControllerRegistration addViewController(String urlPathOrPattern) {
		ViewControllerRegistration registration = new ViewControllerRegistration(urlPathOrPattern);
		registration.setApplicationContext(this.applicationContext);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * 将视图控制器映射到给定的 URL 路径或模式，以便重定向到另一个 URL。
	 * <p>关于模式语法，当解析模式被
	 * {@link PathMatchConfigurer#setPatternParser 启用}时请参见 {@link PathPattern}，
	 * 否则请参见 {@link AntPathMatcher}。两者语法大体相同，
	 * 但 {@link PathPattern} 更贴合 Web 用途且更高效。
	 * <p>默认情况下，重定向 URL 应相对于当前 ServletContext，
	 * 即相对于 Web 应用根目录。
	 * @since 4.1
	 */
	public RedirectViewControllerRegistration addRedirectViewController(String urlPath, String redirectUrl) {
		RedirectViewControllerRegistration registration = new RedirectViewControllerRegistration(urlPath, redirectUrl);
		registration.setApplicationContext(this.applicationContext);
		this.redirectRegistrations.add(registration);
		return registration;
	}

	/**
	 * 将简单控制器映射到给定的 URL 路径（或模式），以便在不渲染响应体的情况下
	 * 将响应状态设置为给定的状态码。
	 * <p>关于模式语法，当解析模式被
	 * {@link PathMatchConfigurer#setPatternParser 启用}时请参见 {@link PathPattern}，
	 * 否则请参见 {@link AntPathMatcher}。两者语法大体相同，
	 * 但 {@link PathPattern} 更贴合 Web 用途且更高效。
	 * @since 4.1
	 */
	public void addStatusController(String urlPath, HttpStatus statusCode) {
		ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);
		registration.setApplicationContext(this.applicationContext);
		registration.setStatusCode(statusCode);
		registration.getViewController().setStatusOnly(true);
		this.registrations.add(registration);
	}

	/**
	 * 指定用于映射视图控制器的 {@code HandlerMapping} 相对于 Spring MVC 中配置的
	 * 其他处理器映射的排序。
	 * <p>默认设置为 1，即紧跟在排序为 0 的注解控制器之后。
	 */
	public void setOrder(int order) {
		this.order = order;
	}


	/**
	 * 返回包含已注册视图控制器映射的 {@code HandlerMapping}，如果没有注册则返回 {@code null}。
	 * @since 4.3.12
	 */
	@Nullable
	protected SimpleUrlHandlerMapping buildHandlerMapping() {
		if (this.registrations.isEmpty() && this.redirectRegistrations.isEmpty()) {
			return null;
		}

		Map<String, Object> urlMap = new LinkedHashMap<>();
		for (ViewControllerRegistration registration : this.registrations) {
			urlMap.put(registration.getUrlPath(), registration.getViewController());
		}
		for (RedirectViewControllerRegistration registration : this.redirectRegistrations) {
			urlMap.put(registration.getUrlPath(), registration.getViewController());
		}

		return new SimpleUrlHandlerMapping(urlMap, this.order);
	}

}
