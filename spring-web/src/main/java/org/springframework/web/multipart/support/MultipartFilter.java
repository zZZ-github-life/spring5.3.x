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

package org.springframework.web.multipart.support;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

/**
 * Servlet 过滤器，通过根 Web 应用上下文中的 {@link MultipartResolver}
 * 解析 multipart 请求。
 *
 * <p>在 Spring 的根 Web 应用上下文中查找 MultipartResolver。
 * 支持在 {@code web.xml} 中通过 "multipartResolverBeanName" 过滤器初始化参数指定，
 * 默认 Bean 名称为 "filterMultipartResolver"。
 *
 * <p>如果未找到 MultipartResolver Bean，此过滤器将回退到默认的 MultipartResolver：
 * 对于 Servlet 3.0 为 {@link StandardServletMultipartResolver}，
 * 其基于 {@code web.xml} 中的 multipart-config 配置节。
 * 但请注意，目前 Servlet 规范只定义了如何在 Servlet 上启用 multipart 配置，
 * 因此除非 Servlet 容器提供变通方案（例如 Tomcat 的 "allowCasualMultipartParsing" 属性），
 * 否则在 Filter 中很可能无法处理 multipart 请求。
 *
 * <p>MultipartResolver 的查找过程是可定制的：重写此过滤器的
 * {@code lookupMultipartResolver} 方法即可使用自定义的 MultipartResolver 实例，
 * 例如在未使用 Spring Web 应用上下文的情况下。
 * 请注意，查找方法不应在每次调用时都创建新的 MultipartResolver 实例，
 * 而应返回预先构建好的实例的引用。
 *
 * <p>注意：此过滤器是使用 DispatcherServlet 的 MultipartResolver 支持的<b>替代方案</b>，
 * 例如适用于具有自定义 Web 视图、不使用 Spring Web MVC 的 Web 应用，
 * 或适用于在 Spring MVC DispatcherServlet 之前应用的过滤器
 * （例如 {@link org.springframework.web.filter.HiddenHttpMethodFilter}）。
 * 无论如何，此过滤器都不应与 Servlet 特定的 multipart 解析结合使用。
 *
 * @author Juergen Hoeller
 * @since 08.10.2003
 * @see #setMultipartResolverBeanName
 * @see #lookupMultipartResolver
 * @see org.springframework.web.multipart.MultipartResolver
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public class MultipartFilter extends OncePerRequestFilter {

	/**
	 * multipart 解析器 Bean 的默认名称。
	 */
	public static final String DEFAULT_MULTIPART_RESOLVER_BEAN_NAME = "filterMultipartResolver";

	private final MultipartResolver defaultMultipartResolver = new StandardServletMultipartResolver();

	private String multipartResolverBeanName = DEFAULT_MULTIPART_RESOLVER_BEAN_NAME;


	/**
	 * 设置要从 Spring 根应用上下文中获取的 MultipartResolver 的 Bean 名称。
	 * 默认值为 "filterMultipartResolver"。
	 */
	public void setMultipartResolverBeanName(String multipartResolverBeanName) {
		this.multipartResolverBeanName = multipartResolverBeanName;
	}

	/**
	 * 返回要从 Spring 根应用上下文中获取的 MultipartResolver 的 Bean 名称。
	 */
	protected String getMultipartResolverBeanName() {
		return this.multipartResolverBeanName;
	}


	/**
	 * 通过此过滤器的 MultipartResolver 检查是否为 multipart 请求，
	 * 并在适当的情况下将原始请求包装为 MultipartHttpServletRequest。
	 * <p>过滤器链中的所有后续元素（最重要的是 Servlet）都将受益于
	 * multipart 情况下正确的参数提取，并且可以在需要时转换为
	 * MultipartHttpServletRequest。
	 */
	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		MultipartResolver multipartResolver = lookupMultipartResolver(request);

		HttpServletRequest processedRequest = request;
		if (multipartResolver.isMultipart(processedRequest)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Resolving multipart request");
			}
			processedRequest = multipartResolver.resolveMultipart(processedRequest);
		}
		else {
			// A regular request...
			if (logger.isTraceEnabled()) {
				logger.trace("Not a multipart request");
			}
		}

		try {
			filterChain.doFilter(processedRequest, response);
		}
		finally {
			if (processedRequest instanceof MultipartHttpServletRequest) {
				multipartResolver.cleanupMultipart((MultipartHttpServletRequest) processedRequest);
			}
		}
	}

	/**
	 * 查找此过滤器应使用的 MultipartResolver，
	 * 以当前 HTTP 请求作为参数。
	 * <p>默认实现委托给不带参数的 {@code lookupMultipartResolver}。
	 * @return the MultipartResolver to use
	 * @see #lookupMultipartResolver()
	 */
	protected MultipartResolver lookupMultipartResolver(HttpServletRequest request) {
		return lookupMultipartResolver();
	}

	/**
	 * 在根 Web 应用上下文中查找 MultipartResolver Bean。
	 * 支持 "multipartResolverBeanName" 过滤器初始化参数；
	 * 默认 Bean 名称为 "filterMultipartResolver"。
	 * <p>可以重写此方法以使用自定义的 MultipartResolver 实例，
	 * 例如在未使用 Spring Web 应用上下文的情况下。
	 * @return the MultipartResolver instance
	 */
	protected MultipartResolver lookupMultipartResolver() {
		WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		String beanName = getMultipartResolverBeanName();
		if (wac != null && wac.containsBean(beanName)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using MultipartResolver '" + beanName + "' for MultipartFilter");
			}
			return wac.getBean(beanName, MultipartResolver.class);
		}
		else {
			return this.defaultMultipartResolver;
		}
	}

}
