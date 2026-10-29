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

package org.springframework.web.servlet.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * URL 映射的 {@link HandlerMapping} 实现的抽象基类。
 *
 * <p>支持字面匹配和模式匹配，例如 "/test/*"、"/test/**"
 * 等。有关模式语法的详情，当解析模式
 * {@link #usesPathPatterns() 启用}时请参阅 {@link PathPattern}，
 * 否则请参阅 {@link AntPathMatcher}。两者语法大致相同，但
 * {@code PathPattern} 语法更针对 Web 应用程序定制，其
 * 实现也更高效。
 *
 * <p>会检查所有路径模式，以找到与当前请求路径最精确的匹配，
 * 其中“最精确”是指与当前请求路径匹配的最长路径模式。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 16.04.2003
 */
public abstract class AbstractUrlHandlerMapping extends AbstractHandlerMapping implements MatchableHandlerMapping {

	@Nullable
	private Object rootHandler;

	private boolean useTrailingSlashMatch = false;

	private boolean lazyInitHandlers = false;

	private final Map<String, Object> handlerMap = new LinkedHashMap<>();

	private final Map<PathPattern, Object> pathPatternHandlerMap = new LinkedHashMap<>();


	@Override
	public void setPatternParser(PathPatternParser patternParser) {
		Assert.state(this.handlerMap.isEmpty(),
				"PathPatternParser must be set before the initialization of " +
						"the handler map via ApplicationContextAware#setApplicationContext.");
		super.setPatternParser(patternParser);
	}

	/**
	 * 为此处理器映射设置根处理器，即为根路径（"/"）
	 * 注册的处理器。
	 * <p>默认值为 {@code null}，表示没有根处理器。
	 */
	public void setRootHandler(@Nullable Object rootHandler) {
		this.rootHandler = rootHandler;
	}

	/**
	 * 返回此处理器映射的根处理器（为 "/" 注册的处理器），
	 * 如果没有则返回 {@code null}。
	 */
	@Nullable
	public Object getRootHandler() {
		return this.rootHandler;
	}

	/**
	 * 是否匹配 URL 时忽略是否存在尾部斜杠。
	 * 如果启用，则诸如 "/users" 的 URL 模式也会匹配 "/users/"。
	 * <p>默认值为 {@code false}。
	 */
	public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
		this.useTrailingSlashMatch = useTrailingSlashMatch;
		if (getPatternParser() != null) {
			getPatternParser().setMatchOptionalTrailingSeparator(useTrailingSlashMatch);
		}
	}

	/**
	 * 是否匹配 URL 时忽略是否存在尾部斜杠。
	 */
	public boolean useTrailingSlashMatch() {
		return this.useTrailingSlashMatch;
	}

	/**
	 * 设置是否延迟初始化处理器。仅适用于
	 * 单例处理器，因为原型始终是延迟初始化的。
	 * 默认值为 "false"，因为通过直接引用控制器对象，
	 * 立即初始化可以提高效率。
	 * <p>如果希望允许控制器延迟初始化，
	 * 请将它们设为 "lazy-init" 并将此标志设置为 true。仅将它们设为
	 * "lazy-init" 不会生效，因为在这种情况下它们是通过
	 * 处理器映射中的引用进行初始化的。
	 */
	public void setLazyInitHandlers(boolean lazyInitHandlers) {
		this.lazyInitHandlers = lazyInitHandlers;
	}

	/**
	 * 查找给定请求的 URL 路径对应的处理器。
	 * @param request current HTTP request
	 * @return the handler instance, or {@code null} if none found
	 */
	@Override
	@Nullable
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		String lookupPath = initLookupPath(request);
		Object handler;
		if (usesPathPatterns()) {
			RequestPath path = ServletRequestPathUtils.getParsedRequestPath(request);
			handler = lookupHandler(path, lookupPath, request);
		}
		else {
			handler = lookupHandler(lookupPath, request);
		}
		if (handler == null) {
			// We need to care for the default handler directly, since we need to
			// expose the PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE for it as well.
			Object rawHandler = null;
			if (StringUtils.matchesCharacter(lookupPath, '/')) {
				rawHandler = getRootHandler();
			}
			if (rawHandler == null) {
				rawHandler = getDefaultHandler();
			}
			if (rawHandler != null) {
				// Bean name or resolved handler?
				if (rawHandler instanceof String) {
					String handlerName = (String) rawHandler;
					rawHandler = obtainApplicationContext().getBean(handlerName);
				}
				validateHandler(rawHandler, request);
				handler = buildPathExposingHandler(rawHandler, lookupPath, lookupPath, null);
			}
		}
		return handler;
	}

	/**
	 * 查找给定 URL 路径对应的处理器实例。当解析后的
	 * {@code PathPattern} {@link #usesPathPatterns() 启用}时使用此方法。
	 * @param path the parsed RequestPath
	 * @param lookupPath the String lookupPath for checking direct hits
	 * @param request current HTTP request
	 * @return a matching handler, or {@code null} if not found
	 * @since 5.3
	 */
	@Nullable
	protected Object lookupHandler(
			RequestPath path, String lookupPath, HttpServletRequest request) throws Exception {

		Object handler = getDirectMatch(lookupPath, request);
		if (handler != null) {
			return handler;
		}

		// Pattern match?
		List<PathPattern> matches = null;
		for (PathPattern pattern : this.pathPatternHandlerMap.keySet()) {
			if (pattern.matches(path.pathWithinApplication())) {
				matches = (matches != null ? matches : new ArrayList<>());
				matches.add(pattern);
			}
		}
		if (matches == null) {
			return null;
		}
		if (matches.size() > 1) {
			matches.sort(PathPattern.SPECIFICITY_COMPARATOR);
			if (logger.isTraceEnabled()) {
				logger.trace("Matching patterns " + matches);
			}
		}
		PathPattern pattern = matches.get(0);
		handler = this.pathPatternHandlerMap.get(pattern);
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = obtainApplicationContext().getBean(handlerName);
		}
		validateHandler(handler, request);
		String pathWithinMapping = pattern.extractPathWithinPattern(path.pathWithinApplication()).value();
		pathWithinMapping = UrlPathHelper.defaultInstance.removeSemicolonContent(pathWithinMapping);
		return buildPathExposingHandler(handler, pattern.getPatternString(), pathWithinMapping, null);
	}

	/**
	 * 查找给定 URL 路径对应的处理器实例。当使用
	 * {@code PathMatcher} 进行字符串模式匹配时使用此方法。
	 * @param lookupPath the path to match patterns against
	 * @param request current HTTP request
	 * @return a matching handler, or {@code null} if not found
	 * @see #exposePathWithinMapping
	 * @see AntPathMatcher
	 */
	@Nullable
	protected Object lookupHandler(String lookupPath, HttpServletRequest request) throws Exception {
		Object handler = getDirectMatch(lookupPath, request);
		if (handler != null) {
			return handler;
		}

		// Pattern match?
		List<String> matchingPatterns = new ArrayList<>();
		for (String registeredPattern : this.handlerMap.keySet()) {
			if (getPathMatcher().match(registeredPattern, lookupPath)) {
				matchingPatterns.add(registeredPattern);
			}
			else if (useTrailingSlashMatch()) {
				if (!registeredPattern.endsWith("/") && getPathMatcher().match(registeredPattern + "/", lookupPath)) {
					matchingPatterns.add(registeredPattern + "/");
				}
			}
		}

		String bestMatch = null;
		Comparator<String> patternComparator = getPathMatcher().getPatternComparator(lookupPath);
		if (!matchingPatterns.isEmpty()) {
			matchingPatterns.sort(patternComparator);
			if (logger.isTraceEnabled() && matchingPatterns.size() > 1) {
				logger.trace("Matching patterns " + matchingPatterns);
			}
			bestMatch = matchingPatterns.get(0);
		}
		if (bestMatch != null) {
			handler = this.handlerMap.get(bestMatch);
			if (handler == null) {
				if (bestMatch.endsWith("/")) {
					handler = this.handlerMap.get(bestMatch.substring(0, bestMatch.length() - 1));
				}
				if (handler == null) {
					throw new IllegalStateException(
							"Could not find handler for best pattern match [" + bestMatch + "]");
				}
			}
			// Bean name or resolved handler?
			if (handler instanceof String) {
				String handlerName = (String) handler;
				handler = obtainApplicationContext().getBean(handlerName);
			}
			validateHandler(handler, request);
			String pathWithinMapping = getPathMatcher().extractPathWithinPattern(bestMatch, lookupPath);

			// There might be multiple 'best patterns', let's make sure we have the correct URI template variables
			// for all of them
			Map<String, String> uriTemplateVariables = new LinkedHashMap<>();
			for (String matchingPattern : matchingPatterns) {
				if (patternComparator.compare(bestMatch, matchingPattern) == 0) {
					Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchingPattern, lookupPath);
					Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
					uriTemplateVariables.putAll(decodedVars);
				}
			}
			if (logger.isTraceEnabled() && uriTemplateVariables.size() > 0) {
				logger.trace("URI variables " + uriTemplateVariables);
			}
			return buildPathExposingHandler(handler, bestMatch, pathWithinMapping, uriTemplateVariables);
		}

		// No handler found...
		return null;
	}

	@Nullable
	private Object getDirectMatch(String urlPath, HttpServletRequest request) throws Exception {
		Object handler = this.handlerMap.get(urlPath);
		if (handler != null) {
			// Bean name or resolved handler?
			if (handler instanceof String) {
				String handlerName = (String) handler;
				handler = obtainApplicationContext().getBean(handlerName);
			}
			validateHandler(handler, request);
			return buildPathExposingHandler(handler, urlPath, urlPath, null);
		}
		return null;
	}

	/**
	 * 针对当前请求验证给定处理器。
	 * <p>默认实现为空。可以在子类中重写，
	 * 例如强制执行 URL 映射中表达的特定前提条件。
	 * @param handler the handler object to validate
	 * @param request current HTTP request
	 * @throws Exception if validation failed
	 */
	protected void validateHandler(Object handler, HttpServletRequest request) throws Exception {
	}

	/**
	 * 为给定原始处理器构建处理器对象，在执行处理器之前
	 * 暴露实际处理器、{@link #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}
	 * 以及 {@link #URI_TEMPLATE_VARIABLES_ATTRIBUTE}。
	 * <p>默认实现使用一个特殊的拦截器构建 {@link HandlerExecutionChain}，
	 * 该拦截器负责暴露路径属性和 URI
	 * 模板变量
	 * @param rawHandler the raw handler to expose
	 * @param pathWithinMapping the path to expose before executing the handler
	 * @param uriTemplateVariables the URI template variables, can be {@code null} if no variables found
	 * @return the final handler object
	 */
	protected Object buildPathExposingHandler(Object rawHandler, String bestMatchingPattern,
			String pathWithinMapping, @Nullable Map<String, String> uriTemplateVariables) {

		HandlerExecutionChain chain = new HandlerExecutionChain(rawHandler);
		chain.addInterceptor(new PathExposingHandlerInterceptor(bestMatchingPattern, pathWithinMapping));
		if (!CollectionUtils.isEmpty(uriTemplateVariables)) {
			chain.addInterceptor(new UriTemplateVariablesHandlerInterceptor(uriTemplateVariables));
		}
		return chain;
	}

	/**
	 * 将当前映射内的路径作为请求属性暴露。
	 * @param pathWithinMapping the path within the current mapping
	 * @param request the request to expose the path to
	 * @see #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE
	 */
	protected void exposePathWithinMapping(String bestMatchingPattern, String pathWithinMapping,
			HttpServletRequest request) {

		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestMatchingPattern);
		request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
	}

	/**
	 * 将 URI 模板变量作为请求属性暴露。
	 * @param uriTemplateVariables the URI template variables
	 * @param request the request to expose the path to
	 * @see #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE
	 */
	protected void exposeUriTemplateVariables(Map<String, String> uriTemplateVariables, HttpServletRequest request) {
		request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);
	}

	@Override
	@Nullable
	public RequestMatchResult match(HttpServletRequest request, String pattern) {
		Assert.isNull(getPatternParser(), "This HandlerMapping uses PathPatterns.");
		String lookupPath = UrlPathHelper.getResolvedLookupPath(request);
		if (getPathMatcher().match(pattern, lookupPath)) {
			return new RequestMatchResult(pattern, lookupPath, getPathMatcher());
		}
		else if (useTrailingSlashMatch()) {
			if (!pattern.endsWith("/") && getPathMatcher().match(pattern + "/", lookupPath)) {
				return new RequestMatchResult(pattern + "/", lookupPath, getPathMatcher());
			}
		}
		return null;
	}

	/**
	 * 为给定 URL 路径注册指定处理器。
	 * @param urlPaths the URLs that the bean should be mapped to
	 * @param beanName the name of the handler bean
	 * @throws BeansException if the handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	protected void registerHandler(String[] urlPaths, String beanName) throws BeansException, IllegalStateException {
		Assert.notNull(urlPaths, "URL path array must not be null");
		for (String urlPath : urlPaths) {
			registerHandler(urlPath, beanName);
		}
	}

	/**
	 * 为给定 URL 路径注册指定处理器。
	 * @param urlPath the URL the bean should be mapped to
	 * @param handler the handler instance or handler bean name String
	 * (a bean name will automatically be resolved into the corresponding handler bean)
	 * @throws BeansException if the handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	protected void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
		Assert.notNull(urlPath, "URL path must not be null");
		Assert.notNull(handler, "Handler object must not be null");
		Object resolvedHandler = handler;

		// Eagerly resolve handler if referencing singleton via name.
		if (!this.lazyInitHandlers && handler instanceof String) {
			String handlerName = (String) handler;
			ApplicationContext applicationContext = obtainApplicationContext();
			if (applicationContext.isSingleton(handlerName)) {
				resolvedHandler = applicationContext.getBean(handlerName);
			}
		}

		Object mappedHandler = this.handlerMap.get(urlPath);
		if (mappedHandler != null) {
			if (mappedHandler != resolvedHandler) {
				throw new IllegalStateException(
						"Cannot map " + getHandlerDescription(handler) + " to URL path [" + urlPath +
						"]: There is already " + getHandlerDescription(mappedHandler) + " mapped.");
			}
		}
		else {
			if (urlPath.equals("/")) {
				if (logger.isTraceEnabled()) {
					logger.trace("Root mapping to " + getHandlerDescription(handler));
				}
				setRootHandler(resolvedHandler);
			}
			else if (urlPath.equals("/*")) {
				if (logger.isTraceEnabled()) {
					logger.trace("Default mapping to " + getHandlerDescription(handler));
				}
				setDefaultHandler(resolvedHandler);
			}
			else {
				this.handlerMap.put(urlPath, resolvedHandler);
				if (getPatternParser() != null) {
					this.pathPatternHandlerMap.put(getPatternParser().parse(urlPath), resolvedHandler);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Mapped [" + urlPath + "] onto " + getHandlerDescription(handler));
				}
			}
		}
	}

	private String getHandlerDescription(Object handler) {
		return (handler instanceof String ? "'" + handler + "'" : handler.toString());
	}


	/**
	 * 以只读 Map 形式返回处理器映射，以已注册的路径
	 * 或模式为键，以处理器对象（如果是延迟初始化的处理器，
	 * 则为处理器 bean 名称）为值。
	 * @see #getDefaultHandler()
	 */
	public final Map<String, Object> getHandlerMap() {
		return Collections.unmodifiableMap(this.handlerMap);
	}

	/**
	 * 与 {@link #getHandlerMap()} 相同，但在解析模式
	 * {@link #usesPathPatterns() 启用}时才会填充；否则为空。
	 * @since 5.3
	 */
	public final Map<PathPattern, Object> getPathPatternHandlerMap() {
		return (this.pathPatternHandlerMap.isEmpty() ?
				Collections.emptyMap() : Collections.unmodifiableMap(this.pathPatternHandlerMap));
	}

	/**
	 * 指示此处理器映射是否支持类型级映射。默认为 {@code false}。
	 */
	protected boolean supportsTypeLevelMappings() {
		return false;
	}


	/**
	 * 用于暴露
	 * {@link AbstractUrlHandlerMapping#PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE} 属性的特殊拦截器。
	 * @see AbstractUrlHandlerMapping#exposePathWithinMapping
	 */
	private class PathExposingHandlerInterceptor implements HandlerInterceptor {

		private final String bestMatchingPattern;

		private final String pathWithinMapping;

		public PathExposingHandlerInterceptor(String bestMatchingPattern, String pathWithinMapping) {
			this.bestMatchingPattern = bestMatchingPattern;
			this.pathWithinMapping = pathWithinMapping;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			exposePathWithinMapping(this.bestMatchingPattern, this.pathWithinMapping, request);
			request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, handler);
			request.setAttribute(INTROSPECT_TYPE_LEVEL_MAPPING, supportsTypeLevelMappings());
			return true;
		}

	}

	/**
	 * 用于暴露
	 * {@link AbstractUrlHandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE} 属性的特殊拦截器。
	 * @see AbstractUrlHandlerMapping#exposePathWithinMapping
	 */
	private class UriTemplateVariablesHandlerInterceptor implements HandlerInterceptor {

		private final Map<String, String> uriTemplateVariables;

		public UriTemplateVariablesHandlerInterceptor(Map<String, String> uriTemplateVariables) {
			this.uriTemplateVariables = uriTemplateVariables;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			exposeUriTemplateVariables(this.uriTemplateVariables, request);
			return true;
		}
	}

}
