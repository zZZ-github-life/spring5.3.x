/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * {@link org.springframework.web.servlet.HandlerMapping} 实现的抽象基类。
 * 支持排序、默认处理器、处理器拦截器，
 * 包括按路径模式映射的处理器拦截器。
 *
 * <p>注意：此基类<i>不</i>支持暴露
 * {@link #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}。对此属性的支持
 * 由具体子类负责，通常基于请求 URL 映射。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 07.04.2003
 * @see #getHandlerInternal
 * @see #setDefaultHandler
 * @see #setInterceptors
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport
		implements HandlerMapping, Ordered, BeanNameAware {

	/** 用于请求映射的专用“隐藏”日志记录器。 */
	protected final Log mappingsLogger =
			LogDelegateFactory.getHiddenLog(HandlerMapping.class.getName() + ".Mappings");


	@Nullable
	private Object defaultHandler;

	@Nullable
	private PathPatternParser patternParser;

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private final List<Object> interceptors = new ArrayList<>();

	private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<>();

	@Nullable
	private CorsConfigurationSource corsConfigurationSource;

	private CorsProcessor corsProcessor = new DefaultCorsProcessor();

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	@Nullable
	private String beanName;


	/**
	 * 为此处理器映射设置默认处理器。
	 * 如果未找到特定映射，将返回此处理器。
	 * <p>默认值为 {@code null}，表示没有默认处理器。
	 */
	public void setDefaultHandler(@Nullable Object defaultHandler) {
		this.defaultHandler = defaultHandler;
	}

	/**
	 * 返回此处理器映射的默认处理器，
	 * 如果没有则返回 {@code null}。
	 */
	@Nullable
	public Object getDefaultHandler() {
		return this.defaultHandler;
	}

	/**
	 * 启用预先解析的 {@link PathPattern}，作为
	 * 使用 {@link AntPathMatcher} 进行字符串模式匹配的替代方案。两者语法
	 * 大致相同，但 {@code PathPattern} 语法更针对
	 * Web 应用程序定制，其实现也更高效。
	 * <p>此属性与以下其他属性互斥，设置此属性后
	 * 这些属性实际上会被忽略：
	 * <ul>
	 * <li>{@link #setAlwaysUseFullPath} -- {@code PathPatterns} 始终使用
	 * 完整路径，并忽略 servletPath/pathInfo，因为它们经过解码和
	 * 部分规范化，因此无法与
	 * {@link HttpServletRequest#getRequestURI() requestURI} 进行比较。
	 * <li>{@link #setRemoveSemicolonContent} -- 出于路径匹配目的，
	 * {@code PathPatterns} 始终忽略分号内容，但路径参数
	 * 仍可在控制器中通过 {@code @MatrixVariable} 使用。
	 * <li>{@link #setUrlDecode} -- {@code PathPatterns} 一次匹配一个已解码的路径
	 * 段，永远不需要完整的已解码路径，而完整解码路径可能
	 * 因解码后的保留字符而引发问题。
	 * <li>{@link #setUrlPathHelper} -- 请求路径由
	 * {@link org.springframework.web.servlet.DispatcherServlet
	 * DispatcherServlet} 或
	 * {@link org.springframework.web.filter.ServletRequestPathFilter
	 * ServletRequestPathFilter} 使用 {@link ServletRequestPathUtils} 在全局范围内
	 * 预先解析，并保存在请求属性中以供重复使用。
	 * <li>{@link #setPathMatcher} -- 模式会被解析为 {@code PathPatterns}
	 * 并使用它们，而不是使用 {@code PathMatcher} 进行字符串匹配。
	 * </ul>
	 * <p>默认情况下不设置此属性。
	 * @param patternParser the parser to use
	 * @since 5.3
	 */
	public void setPatternParser(PathPatternParser patternParser) {
		this.patternParser = patternParser;
	}

	/**
	 * 返回{@link #setPatternParser(PathPatternParser) 配置}的
	 * {@code PathPatternParser}，如果没有则为 {@code null}。
	 * @since 5.3
	 */
	@Nullable
	public PathPatternParser getPatternParser() {
		return this.patternParser;
	}

	/**
	 * 所配置的 {@code UrlPathHelper} 上同一属性的快捷方式。
	 * <p><strong>注意：</strong>此属性与
	 * {@link #setPatternParser(PathPatternParser)} 互斥，设置后者时此属性会被忽略。
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath(boolean)
	 */
	@SuppressWarnings("deprecation")
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setAlwaysUseFullPath(alwaysUseFullPath);
		}
	}

	/**
	 * 底层 {@code UrlPathHelper} 上同一属性的快捷方式。
	 * <p><strong>注意：</strong>此属性与
	 * {@link #setPatternParser(PathPatternParser)} 互斥，设置后者时此属性会被忽略。
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode(boolean)
	 */
	@SuppressWarnings("deprecation")
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setUrlDecode(urlDecode);
		}
	}

	/**
	 * 底层 {@code UrlPathHelper} 上同一属性的快捷方式。
	 * <p><strong>注意：</strong>此属性与
	 * {@link #setPatternParser(PathPatternParser)} 互斥，设置后者时此属性会被忽略。
	 * @see org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent(boolean)
	 */
	@SuppressWarnings("deprecation")
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setRemoveSemicolonContent(removeSemicolonContent);
		}
	}

	/**
	 * 配置用于解析查找路径的 UrlPathHelper。
	 * <p><strong>注意：</strong>此属性与
	 * {@link #setPatternParser(PathPatternParser)} 互斥，设置后者时此属性会被忽略。
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setUrlPathHelper(urlPathHelper);
		}
	}

	/**
	 * 返回{@link #setUrlPathHelper 配置}的 {@code UrlPathHelper}。
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * 配置要使用的 PathMatcher。
	 * <p><strong>注意：</strong>此属性与
	 * {@link #setPatternParser(PathPatternParser)} 互斥，设置后者时此属性会被忽略。
	 * <p>默认情况下为 {@link AntPathMatcher}。
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setPathMatcher(pathMatcher);
		}
	}

	/**
	 * 返回{@link #setPathMatcher 配置}的 {@code PathMatcher}。
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 设置要应用于此处理器映射所映射的所有处理器的拦截器。
	 * <p>支持的拦截器类型为 {@link HandlerInterceptor}、
	 * {@link WebRequestInterceptor} 和 {@link MappedInterceptor}。
	 * 映射拦截器仅应用于其路径模式匹配的请求 URL。
	 * 在初始化期间还会按类型检测映射拦截器 bean。
	 * @param interceptors array of handler interceptors
	 * @see #adaptInterceptor
	 * @see org.springframework.web.servlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 * @see MappedInterceptor
	 */
	public void setInterceptors(Object... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}

	/**
	 * 设置“全局”CORS 配置映射。第一个匹配的 URL 模式
	 * 决定要使用的 {@code CorsConfiguration}，然后将其与
	 * 所选处理器的 {@code CorsConfiguration}
	 * {@link CorsConfiguration#combine(CorsConfiguration) 合并}。
	 * <p>此属性与
	 * {@link #setCorsConfigurationSource(CorsConfigurationSource)} 互斥。
	 * @since 4.2
	 * @see #setCorsProcessor(CorsProcessor)
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		if (CollectionUtils.isEmpty(corsConfigurations)) {
			this.corsConfigurationSource = null;
			return;
		}
		UrlBasedCorsConfigurationSource source;
		if (getPatternParser() != null) {
			source = new UrlBasedCorsConfigurationSource(getPatternParser());
			source.setCorsConfigurations(corsConfigurations);
		}
		else {
			source = new UrlBasedCorsConfigurationSource();
			source.setCorsConfigurations(corsConfigurations);
			source.setPathMatcher(this.pathMatcher);
			source.setUrlPathHelper(this.urlPathHelper);
		}
		setCorsConfigurationSource(source);
	}

	/**
	 * 为“全局”CORS 配置设置一个 {@code CorsConfigurationSource}。由该源
	 * 确定的 {@code CorsConfiguration} 会与
	 * 所选处理器的 {@code CorsConfiguration}
	 * {@link CorsConfiguration#combine(CorsConfiguration) 合并}。
	 * <p>此属性与 {@link #setCorsConfigurations(Map)} 互斥。
	 * @since 5.1
	 * @see #setCorsProcessor(CorsProcessor)
	 */
	public void setCorsConfigurationSource(CorsConfigurationSource source) {
		Assert.notNull(source, "CorsConfigurationSource must not be null");
		this.corsConfigurationSource = source;
		if (source instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) source).setAllowInitLookupPath(false);
		}
	}

	/**
	 * 返回{@link #setCorsConfigurationSource(CorsConfigurationSource)
	 * 配置}的 {@code CorsConfigurationSource}（如果有）。
	 * @since 5.3
	 */
	@Nullable
	public CorsConfigurationSource getCorsConfigurationSource() {
		return this.corsConfigurationSource;
	}

	/**
	 * 配置自定义 {@link CorsProcessor}，用于为请求应用匹配的
	 * {@link CorsConfiguration}。
	 * <p>默认使用 {@link DefaultCorsProcessor}。
	 * @since 4.2
	 */
	public void setCorsProcessor(CorsProcessor corsProcessor) {
		Assert.notNull(corsProcessor, "CorsProcessor must not be null");
		this.corsProcessor = corsProcessor;
	}

	/**
	 * 返回所配置的 {@link CorsProcessor}。
	 */
	public CorsProcessor getCorsProcessor() {
		return this.corsProcessor;
	}

	/**
	 * 指定此 HandlerMapping bean 的排序值。
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

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	protected String formatMappingName() {
		return this.beanName != null ? "'" + this.beanName + "'" : getClass().getName();
	}


	/**
	 * 初始化拦截器。
	 * @see #extendInterceptors(java.util.List)
	 * @see #initInterceptors()
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		extendInterceptors(this.interceptors);
		detectMappedInterceptors(this.adaptedInterceptors);
		initInterceptors();
	}

	/**
	 * 扩展钩子，子类可以重写它以在给定已配置拦截器（参见
	 * {@link #setInterceptors}）的基础上注册额外的拦截器。
	 * <p>将在 {@link #initInterceptors()} 将指定拦截器适配为
	 * {@link HandlerInterceptor} 实例之前调用。
	 * <p>默认实现为空。
	 * @param interceptors the configured interceptor List (never {@code null}), allowing
	 * to add further interceptors before as well as after the existing interceptors
	 */
	protected void extendInterceptors(List<Object> interceptors) {
	}

	/**
	 * 检测 {@link MappedInterceptor} 类型的 bean，并将其添加到
	 * 映射拦截器列表中。
	 * <p>除了可能通过 {@link #setInterceptors} 提供的任何
	 * {@link MappedInterceptor} 之外，还会调用此方法，默认情况下会添加来自当前上下文及其
	 * 祖先上下文的所有 {@link MappedInterceptor} 类型的 bean。
	 * 子类可以重写并细化此策略。
	 * @param mappedInterceptors an empty list to add to
	 */
	protected void detectMappedInterceptors(List<HandlerInterceptor> mappedInterceptors) {
		mappedInterceptors.addAll(BeanFactoryUtils.beansOfTypeIncludingAncestors(
				obtainApplicationContext(), MappedInterceptor.class, true, false).values());
	}

	/**
	 * 初始化指定的拦截器，将
	 * {@link WebRequestInterceptor} 适配为 {@link HandlerInterceptor}。
	 * @see #setInterceptors
	 * @see #adaptInterceptor
	 */
	protected void initInterceptors() {
		if (!this.interceptors.isEmpty()) {
			for (int i = 0; i < this.interceptors.size(); i++) {
				Object interceptor = this.interceptors.get(i);
				if (interceptor == null) {
					throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
				}
				this.adaptedInterceptors.add(adaptInterceptor(interceptor));
			}
		}
	}

	/**
	 * 将给定拦截器对象适配为 {@link HandlerInterceptor}。
	 * <p>默认情况下，支持的拦截器类型为
	 * {@link HandlerInterceptor} 和 {@link WebRequestInterceptor}。每个给定的
	 * {@link WebRequestInterceptor} 都会使用
	 * {@link WebRequestHandlerInterceptorAdapter} 包装。
	 * @param interceptor the interceptor
	 * @return the interceptor downcast or adapted to HandlerInterceptor
	 * @see org.springframework.web.servlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 * @see WebRequestHandlerInterceptorAdapter
	 */
	protected HandlerInterceptor adaptInterceptor(Object interceptor) {
		if (interceptor instanceof HandlerInterceptor) {
			return (HandlerInterceptor) interceptor;
		}
		else if (interceptor instanceof WebRequestInterceptor) {
			return new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor) interceptor);
		}
		else {
			throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
		}
	}

	/**
	 * 以 {@link HandlerInterceptor} 数组形式返回适配后的拦截器。
	 * @return the array of {@link HandlerInterceptor HandlerInterceptor}s,
	 * or {@code null} if none
	 */
	@Nullable
	protected final HandlerInterceptor[] getAdaptedInterceptors() {
		return (!this.adaptedInterceptors.isEmpty() ?
				this.adaptedInterceptors.toArray(new HandlerInterceptor[0]) : null);
	}

	/**
	 * 以数组形式返回所有已配置的 {@link MappedInterceptor}。
	 * @return the array of {@link MappedInterceptor}s, or {@code null} if none
	 */
	@Nullable
	protected final MappedInterceptor[] getMappedInterceptors() {
		List<MappedInterceptor> mappedInterceptors = new ArrayList<>(this.adaptedInterceptors.size());
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				mappedInterceptors.add((MappedInterceptor) interceptor);
			}
		}
		return (!mappedInterceptors.isEmpty() ? mappedInterceptors.toArray(new MappedInterceptor[0]) : null);
	}


	/**
	 * 如果此 {@code HandlerMapping} 已{@link #setPatternParser 启用}
	 * 使用解析后的 {@code PathPattern}，则返回 "true"。
	 */
	@Override
	public boolean usesPathPatterns() {
		return getPatternParser() != null;
	}

	/**
	 * 查找给定请求的处理器，如果未找到特定处理器，
	 * 则回退到默认处理器。
	 * @param request current HTTP request
	 * @return the corresponding handler instance, or the default handler
	 * @see #getHandlerInternal
	 */
	@Override
	@Nullable
	public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		Object handler = getHandlerInternal(request);
		if (handler == null) {
			handler = getDefaultHandler();
		}
		if (handler == null) {
			return null;
		}
		// Bean name or resolved handler?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = obtainApplicationContext().getBean(handlerName);
		}

		// Ensure presence of cached lookupPath for interceptors and others
		if (!ServletRequestPathUtils.hasCachedPath(request)) {
			initLookupPath(request);
		}

		HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);

		if (logger.isTraceEnabled()) {
			logger.trace("Mapped to " + handler);
		}
		else if (logger.isDebugEnabled() && !DispatcherType.ASYNC.equals(request.getDispatcherType())) {
			logger.debug("Mapped to " + executionChain.getHandler());
		}

		if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {
			CorsConfiguration config = getCorsConfiguration(handler, request);
			if (getCorsConfigurationSource() != null) {
				CorsConfiguration globalConfig = getCorsConfigurationSource().getCorsConfiguration(request);
				config = (globalConfig != null ? globalConfig.combine(config) : config);
			}
			if (config != null) {
				config.validateAllowCredentials();
			}
			executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
		}

		return executionChain;
	}

	/**
	 * 查找给定请求的处理器，如果未找到特定处理器，
	 * 则返回 {@code null}。此方法由 {@link #getHandler} 调用；
	 * 返回 {@code null} 将导致使用默认处理器（如果已设置）。
	 * <p>对于 CORS 预检请求，此方法不应返回针对预检请求本身的匹配，
	 * 而应基于 URL 路径、"Access-Control-Request-Method" 请求头中的
	 * HTTP 方法以及 "Access-Control-Request-Headers" 请求头中的请求头，
	 * 返回针对预期实际请求的匹配，从而允许
	 * 通过 {@link #getCorsConfiguration(Object, HttpServletRequest)} 获取 CORS 配置，
	 * <p>注意：此方法也可以返回预先构建的 {@link HandlerExecutionChain}，
	 * 将处理器对象与动态确定的拦截器组合在一起。
	 * 静态指定的拦截器将合并到此类现有链中。
	 * @param request current HTTP request
	 * @return the corresponding handler instance, or {@code null} if none found
	 * @throws Exception if there is an internal error
	 */
	@Nullable
	protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;

	/**
	 * 初始化用于请求映射的路径。
	 * <p>当解析模式 {@link #usesPathPatterns() 启用}时，预期已解析的
	 * {@code RequestPath} 已由
	 * {@link org.springframework.web.servlet.DispatcherServlet}
	 * 或 {@link org.springframework.web.filter.ServletRequestPathFilter}
	 * 在外部
	 * {@link ServletRequestPathUtils#parseAndCache(HttpServletRequest) 解析}。
	 * <p>否则，对于通过 {@code PathMatcher} 进行的字符串模式匹配，
	 * 路径由本方法
	 * {@link UrlPathHelper#resolveAndCacheLookupPath 解析}。
	 * @since 5.3
	 */
	protected String initLookupPath(HttpServletRequest request) {
		if (usesPathPatterns()) {
			request.removeAttribute(UrlPathHelper.PATH_ATTRIBUTE);
			RequestPath requestPath = ServletRequestPathUtils.getParsedRequestPath(request);
			String lookupPath = requestPath.pathWithinApplication().value();
			return UrlPathHelper.defaultInstance.removeSemicolonContent(lookupPath);
		}
		else {
			return getUrlPathHelper().resolveAndCacheLookupPath(request);
		}
	}

	/**
	 * 为给定处理器构建 {@link HandlerExecutionChain}，包括
	 * 适用的拦截器。
	 * <p>默认实现使用给定处理器、处理器映射的公共拦截器以及任何
	 * 匹配当前请求 URL 的 {@link MappedInterceptor MappedInterceptors}
	 * 构建标准的 {@link HandlerExecutionChain}。拦截器
	 * 按注册顺序添加。子类可以重写此方法
	 * 以扩展/重新排列拦截器列表。
	 * <p><b>注意：</b>传入的处理器对象可以是原始处理器，
	 * 也可以是预先构建的 {@link HandlerExecutionChain}。此方法应显式
	 * 处理这两种情况：要么构建新的 {@link HandlerExecutionChain}，
	 * 要么扩展现有链。
	 * <p>如果只想在自定义子类中添加一个拦截器，可以考虑调用
	 * {@code super.getHandlerExecutionChain(handler, request)}，并在返回的链对象上
	 * 调用 {@link HandlerExecutionChain#addInterceptor}。
	 * @param handler the resolved handler instance (never {@code null})
	 * @param request current HTTP request
	 * @return the HandlerExecutionChain (never {@code null})
	 * @see #getAdaptedInterceptors()
	 */
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
		HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
				(HandlerExecutionChain) handler : new HandlerExecutionChain(handler));

		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
				if (mappedInterceptor.matches(request)) {
					chain.addInterceptor(mappedInterceptor.getInterceptor());
				}
			}
			else {
				chain.addInterceptor(interceptor);
			}
		}
		return chain;
	}

	/**
	 * 如果此处理器存在 {@link CorsConfigurationSource}，则返回 {@code true}。
	 * @since 5.2
	 */
	protected boolean hasCorsConfigurationSource(Object handler) {
		if (handler instanceof HandlerExecutionChain) {
			handler = ((HandlerExecutionChain) handler).getHandler();
		}
		return (handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null);
	}

	/**
	 * 获取给定处理器的 CORS 配置。
	 * @param handler the handler to check (never {@code null}).
	 * @param request the current request.
	 * @return the CORS configuration for the handler, or {@code null} if none
	 * @since 4.2
	 */
	@Nullable
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		Object resolvedHandler = handler;
		if (handler instanceof HandlerExecutionChain) {
			resolvedHandler = ((HandlerExecutionChain) handler).getHandler();
		}
		if (resolvedHandler instanceof CorsConfigurationSource) {
			return ((CorsConfigurationSource) resolvedHandler).getCorsConfiguration(request);
		}
		return null;
	}

	/**
	 * 更新 HandlerExecutionChain 以进行 CORS 相关处理。
	 * <p>对于预检请求，默认实现会将所选处理器替换为简单的
	 * HttpRequestHandler，该处理器会调用所配置的
	 * {@link #setCorsProcessor}。
	 * <p>对于实际请求，默认实现会插入一个
	 * HandlerInterceptor，由它执行 CORS 相关检查并添加 CORS 响应头。
	 * @param request the current request
	 * @param chain the handler chain
	 * @param config the applicable CORS configuration (possibly {@code null})
	 * @since 4.2
	 */
	protected HandlerExecutionChain getCorsHandlerExecutionChain(HttpServletRequest request,
			HandlerExecutionChain chain, @Nullable CorsConfiguration config) {

		if (CorsUtils.isPreFlightRequest(request)) {
			HandlerInterceptor[] interceptors = chain.getInterceptors();
			return new HandlerExecutionChain(new PreFlightHandler(config), interceptors);
		}
		else {
			chain.addInterceptor(0, new CorsInterceptor(config));
			return chain;
		}
	}


	private class PreFlightHandler implements HttpRequestHandler, CorsConfigurationSource {

		@Nullable
		private final CorsConfiguration config;

		public PreFlightHandler(@Nullable CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
			corsProcessor.processRequest(this.config, request, response);
		}

		@Override
		@Nullable
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}


	private class CorsInterceptor implements HandlerInterceptor, CorsConfigurationSource {

		@Nullable
		private final CorsConfiguration config;

		public CorsInterceptor(@Nullable CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
				throws Exception {

			// Consistent with CorsFilter, ignore ASYNC dispatches
			WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
			if (asyncManager.hasConcurrentResult()) {
				return true;
			}

			return corsProcessor.processRequest(this.config, request, response);
		}

		@Override
		@Nullable
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}

}
